package com.mj.users.processor

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.UserDao.getFiendBirthdayDetails
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global


class GetFriendBirthdayDetailsProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  val getFriendBirthdayDetailsProcessor = LoggerFactory.getLogger(this.getClass.getName)

  def receive = {

    case (memberID: String) => {
      val origin = sender()
      val result = getFiendBirthdayDetails(memberID)
        .map(updatedResult => {
          println("updatedResult:"+updatedResult)
          if(updatedResult.isDefined)
          origin ! updatedResult.get.birthdayDetails.filter(p => p.memberID == memberID).head
          else
            origin ! responseMessage("", "", "No Records Found")
        }
        )
      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
