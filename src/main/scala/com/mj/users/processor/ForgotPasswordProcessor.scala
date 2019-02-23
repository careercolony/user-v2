package com.mj.users.processor

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{ForgotPasswordDto, UpdatePasswordDto, responseMessage}
import com.mj.users.mongo.UserDao._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

class ForgotPasswordProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  val forgotPasswordProcessorLog = LoggerFactory.getLogger(this.getClass.getName)

  def receive = {

    case (forgotPasswordDto: ForgotPasswordDto) => {
      val origin = sender()
      val result = forgotPasswordDetails(forgotPasswordDto)
        .map(result =>
          result match {
            case Some(dtoDetails) =>origin ! responseMessage(forgotPasswordDto.email, "", forgotPasswordSuccess)
            case None =>origin ! responseMessage(forgotPasswordDto.email, forgotPasswordFailed, "")

          }

        )
      result.recover {
        case e: Throwable => {
          origin ! responseMessage(forgotPasswordDto.email, e.getMessage, "")
        }
      }
    }
  }
}
