package com.mj.users.processor

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{Interest, UpdatePasswordDto, responseMessage}
import com.mj.users.mongo.UserDao._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

class UpdatePasswordProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  val updatePasswordProcessorLog = LoggerFactory.getLogger(this.getClass.getName)

  def receive = {

    case (updastePasswordDto: UpdatePasswordDto) => {
      val origin = sender()
      val result = updatePasswordDetails(updastePasswordDto)
        .map(updatedResult => origin ! responseMessage(updastePasswordDto.email, "", updatePasswordSuccess)
        )
      result.recover {
        case e: Throwable => {
          origin ! responseMessage(updastePasswordDto.email, e.getMessage, "")
        }
      }
    }
  }
}
