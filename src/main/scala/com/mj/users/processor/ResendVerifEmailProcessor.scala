package com.mj.users.processor

import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{ResendEmailInfo, responseMessage}
import com.mj.users.mongo.UserDao._
import com.mj.users.MailGun._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

class ResendVerifEmailProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  val resendVerifEmailProcessorLog = LoggerFactory.getLogger(this.getClass.getName)

  def receive = {

    case (resendInfo: ResendEmailInfo) => {
      val origin = sender()
      val result = resendVerificationEmail(resendInfo)
         origin ! responseMessage(resendInfo.memberID, "", "Email successfully resent")
     
    }
  }
}
