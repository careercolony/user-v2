package com.mj.users.processor

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{MultipleInvitation, responseMessage}
import com.mj.users.mongo.UserDao.{emailVerification, getUserDetailsById}
import org.json4s.DefaultFormats
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}


class EmailNotificationUserProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  val wfsRescheduleWOLog = LoggerFactory.getLogger(this.getClass.getName)

  def receive = {

    case (memberID: String, system: ActorSystem) => {
      val origin = sender()
      val friendInvitationDispatcher = system.actorSelection("/*/FriendInvitationDispatcher")
      val result = getUserDetailsById(memberID).map(userObj => {
        if (userObj.get.connections_flag.isDefined && !userObj.get.connections_flag.get && userObj.get.secondSignup_flag.isDefined && userObj.get.secondSignup_flag.get && userObj.get.registerDto.connections.isDefined) {
          //calling for friends invitations
          val resp = (friendInvitationDispatcher ? MultipleInvitation(memberID, userObj.get.registerDto.connections)).mapTo[scalaj.http.HttpResponse[String]]
          implicit val formats = DefaultFormats

          resp onComplete {
            case Success(res) => res.code match {
              case 200 => {
                emailVerification(memberID, userObj.get).flatMap(upResult => Future {
                  responseMessage("", "", emailSuccess)
                }).map(response => origin ! response)
              }
              case _ => origin ! responseMessage(memberID, "Error while sending friend request to connections", "")
            }
            case Failure(error) => origin ! responseMessage(memberID, "Error while sending friend request to connections", "")
          }

        } else {
          emailVerification(memberID ,  userObj.get).flatMap(upResult => Future {
            responseMessage("", "", emailSuccess)
          }).map(response => origin ! response)

        }
      })

      result.recover {
        case e: Throwable => {
          origin ! Some(responseMessage("", e.getMessage, ""))
        }

      }
    }

  }
}

