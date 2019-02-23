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
import com.mj.users.tools.{CommonUtils, SchedulingValidator}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.slf4j.LoggerFactory
import spray.json._
import com.mj.users.mongo.KafkaAccess
import scala.util.{Failure, Success}

trait UpdatePasswordRoute extends KafkaAccess{
  val updatePasswordLog = LoggerFactory.getLogger(this.getClass.getName)


  def updatePassword(system: ActorSystem): Route = {

    val updatePasswordProcessor = system.actorSelection("/*/updatePasswordProcessor")
    implicit val timeout = Timeout(20, TimeUnit.SECONDS)


    path("updatePassword") {
      post {
        entity(as[UpdatePasswordDto]) { dto =>
          val validatorResp = SchedulingValidator.validateUpdatePasswordUserRequest(dto)
          validatorResp match {
            case Some(response) => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, validatorResp.toJson.toString)))
            case None =>
              val userResponse = updatePasswordProcessor ? dto
              onComplete(userResponse) {
                case Success(resp) =>
                  resp match {
                    case s: responseMessage  => if (s.successmsg.nonEmpty)
                      complete(HttpResponse(entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                    else
                      complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, s.toJson.toString)))
                    case _ => complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", resp.toString, "").toJson.toString)))
                  }
                case Failure(error) =>
                  updatePasswordLog.error("Error is: " + error.getMessage)
                  complete(HttpResponse(status = BadRequest, entity = HttpEntity(MediaTypes.`application/json`, responseMessage("", error.getMessage, "").toJson.toString)))
              }
          }
        }
      }
    }
  }
}
