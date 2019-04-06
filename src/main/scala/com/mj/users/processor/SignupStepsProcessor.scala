package com.mj.users.processor

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{MultipleInvitation, SecondSignupStep, responseMessage}
import com.mj.users.mongo.UserDao._
import org.json4s.DefaultFormats
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class SignupStepsProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  val signupStepsProcessorLog = LoggerFactory.getLogger(this.getClass.getName)

  def receive = {

    case (secondStepRequest: SecondSignupStep, system: ActorSystem) => {
      val friendInvitationDispatcher = system.actorSelection("/*/FriendInvitationDispatcher")
      val origin = sender()
      println("here")
      val result = getUserDetailsById(secondStepRequest.memberID).flatMap(userObj => {
        updateUserDetails(secondStepRequest, userObj)
          .map(updatedResult =>
            Future {
              insertExperienceDetails(secondStepRequest).map(response => {
                if (userObj.get.email_verification_flag.isDefined && userObj.get.email_verification_flag.get && secondStepRequest.connections.isDefined) {
                  //calling for friends invitations
                  val resp = (friendInvitationDispatcher ? MultipleInvitation(secondStepRequest.memberID, secondStepRequest.connections)).mapTo[scalaj.http.HttpResponse[String]]
                  implicit val formats = DefaultFormats

                  resp onComplete {
                    case Success(res) => res.code match {
                      case 200 => origin ! responseMessage(secondStepRequest.memberID, "", secondSignupSuccess)
                      case _ => origin ! responseMessage(secondStepRequest.memberID, "Error while sending friend request to connections", "")
                    }
                    case Failure(error) => origin ! responseMessage(secondStepRequest.memberID, "Error while sending friend request to connections", "")
                  }

                } else
                  origin ! responseMessage(secondStepRequest.memberID, "", secondSignupSuccess)
              }
              )
            }
          )
      })
      result.recover {
        case e: Throwable => {
          origin ! responseMessage(secondStepRequest.memberID, e.getMessage, "")
        }
      }
    }
  }
}
