package com.mj.users.dispatcher

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, DiagnosticActorLogging}
import akka.util.Timeout
import com.mj.users.config.Application.friendInvitationUrl
import com.mj.users.model.{Consumer, JsonRepo, MultipleInvitation}
import com.mj.users.model.JsonRepo._
import scalaj.http.{Http, HttpResponse}
import spray.json._

import scala.util.Try
import spray.json._
import JsonRepo.multipleInvitationFormats
/*
 *  Created by neluma001c on 8/7/2018
 */

class FriendInvitationDispatcher extends Actor with DiagnosticActorLogging {

  def receive = {
    case invitations : MultipleInvitation =>
      val origin = sender()
      implicit val timeout = Timeout(20, TimeUnit.SECONDS)
      println("show something")
      println("request for friends invitation :"+invitations.toJson.toString)
      val result = Try{Http(friendInvitationUrl).header("Content-Type", "application/json").postData(invitations.toJson.toString).asString}.map(
       response =>  {
         println("resp111: "+response)
         origin ! HttpResponse(response, 200, Map.empty)}
      )

      result.recover {
        case e: Exception => {
         println("e: "+e.getMessage)
          origin ! HttpResponse(e.getMessage, 400, Map.empty)
        }
      }


  }
}
