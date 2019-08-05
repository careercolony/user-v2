package com.mj.users.processor

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{MultipleInvitation, Experience, SecondSignupStep, responseMessage}
import com.mj.users.mongo.UserDao._
import org.json4s.DefaultFormats
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import com.mj.users.mongo.KafkaAccess
import com.mj.users.config.Application._
import scala.util.{Failure, Success}
import com.mj.users.model.JsonRepo._
import spray.json._

class SignupStepsProcessor extends Actor with MessageConfig with KafkaAccess {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  val signupStepsProcessorLog = LoggerFactory.getLogger(this.getClass.getName)

  def receive = {

    case (secondStepRequest: SecondSignupStep, system: ActorSystem) => {
      val friendInvitationDispatcher = system.actorSelection("/*/FriendInvitationDispatcher")
      val origin = sender()
      println("here")
      println(secondStepRequest.memberID)
      println(secondStepRequest.employmentStatus)
      val result = getUserDetailsById(secondStepRequest.memberID).map(userObj => {
        println(secondStepRequest)
        updateUserDetails(secondStepRequest, userObj)
          .map(updatedResult =>
            Future {
              insertExperienceDetails(secondStepRequest).map(response => {
                println("The submitted data" + secondStepRequest)
                sendPostToKafka(response.toString, experienceTopic)
                if (userObj.get.email_verification_flag.isDefined && userObj.get.email_verification_flag.get && secondStepRequest.connections.isDefined) {
                  //calling for friends invitations
                  
                  val resp = (friendInvitationDispatcher ? MultipleInvitation(secondStepRequest.memberID, secondStepRequest.connections)).mapTo[scalaj.http.HttpResponse[String]]
                  implicit val formats = DefaultFormats
                  println(resp)
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




