package com.mj.users.processor

import java.util.concurrent.TimeUnit
import com.mj.users.mongo.UserDao.getUserDetailsById
import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import org.slf4j.LoggerFactory
import scala.concurrent.ExecutionContext.Implicits.global


class GetUserDetailsProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  val getUserDetailsProcessor = LoggerFactory.getLogger(this.getClass.getName)

  def receive = {

    case (memberID: String) => {
      val origin = sender()
      val result = getUserDetailsById(memberID)
        .map(updatedResult => origin ! updatedResult
        )
      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
