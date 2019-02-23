package com.mj.users.route

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import akka.http.scaladsl.server.Directives.{as, complete, entity, path, post, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import com.mj.users.model.JsonRepo._
import com.mj.users.model.{responseMessage, _}
import com.mj.users.mongo.KafkaAccess
import com.mj.users.tools.{CommonUtils, SchedulingValidator}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.slf4j.LoggerFactory
import spray.json._
import com.mj.users.config.Application._
import scala.util.{Failure, Success}

trait ForgotPasswordRoute extends KafkaAccess {
  val forgotPasswordLog = LoggerFactory.getLogger(this.getClass.getName)


  def forgotPassword(system: ActorSystem): Route = {

    val forgotPasswordProcessor = system.actorSelection("/*/forgotPasswordProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)


    path("user" / "forgotPassword") {
      post {
        entity(as[ForgotPasswordDto]) { dto =>
          val validatorResp = SchedulingValidator.validateForgotPasswordUserRequest(dto)
          validatorResp match {
            case Some(response) => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, validatorResp.toJson.toString)))
            case None =>
              val userResponse = forgotPasswordProcessor ? dto
              onComplete(userResponse) {
                case Success(resp) =>
                  resp match {
                    case s: responseMessage  => if (s.successmsg.nonEmpty) {
                      sendPostToKafka(dto.toJson.toString ,forgotTopic)
                      complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                    }else
                      complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                    case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
                  }
                case Failure(error) =>
                  forgotPasswordLog.error("Error is: " + error.getMessage)
                  complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
              }
          }
        }
      }
    }
  }
}
