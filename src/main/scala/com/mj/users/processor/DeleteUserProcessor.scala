package com.mj.users.processor

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.responseMessage
import com.mj.users.mongo.UserDao.updateUserStatusFlag
import com.mj.users.mongo.MongoConnector.remove
import reactivemongo.bson.document

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteUserProcessor extends Actor with MessageConfig{

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)


  def receive = {

    case (memberID: String ) => {
        val origin = sender()
        val result = updateUserStatusFlag(memberID)
          .map(updatedResult => origin ! responseMessage(memberID, "", secondSignupSuccess)
          )
        result.recover {
          case e: Throwable => {
            origin ! responseMessage(memberID, e.getMessage, "")
          }
        }
      }


  }
}
