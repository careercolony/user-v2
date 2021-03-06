package com.mj.users

import java.net.InetAddress

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.routing.RoundRobinPool
import akka.stream.ActorMaterializer
import com.mj.users.config.Application
import com.mj.users.config.Application._
import com.mj.users.tools.CommonUtils._
import com.mj.users.tools.RouteUtils
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

object Server extends App {
  val seedNodesStr = seedNodes
    .split(",")
    .map(s => s""" "akka.tcp://users-cluster@$s" """)
    .mkString(",")

  val inetAddress = InetAddress.getLocalHost
  var configCluster = Application.config.withFallback(
    ConfigFactory.parseString(s"akka.cluster.seed-nodes=[$seedNodesStr]"))

  configCluster = configCluster
    .withFallback(
      ConfigFactory.parseString(s"akka.remote.netty.tcp.hostname=$hostName"))
    .withFallback(
      ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$akkaPort"))

  implicit val system: ActorSystem = ActorSystem("users-cluster", configCluster)
  implicit val materializer: ActorMaterializer = ActorMaterializer()


  val registerProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.RegisterProcessor]), "registerProcessor")
  val loginProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.LoginProcessor]), "loginProcessor")
  val logoutProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.LogoutProcessor]), "logoutProcessor")
  val sigupStepsProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.SignupStepsProcessor]), "signupStepsProcessor")
  val updateInterestProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.UpdateInterestProcessor]), "updateInterestProcessor")
  val updateInfoProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.UpdateInfoProcessor]), "updateInfoProcessor")
  val JWTCredentialsCreation = system.actorOf(RoundRobinPool(20).props(Props[dispatcher.JWTCredentialsCreation]), "JWTCredentialsCreation")
  val JWTConsumerCreation = system.actorOf(RoundRobinPool(20).props(Props[dispatcher.JWTConsumerCreation]), "JWTConsumerCreation")
  val JWTConsumerRemoval = system.actorOf(RoundRobinPool(20).props(Props[dispatcher.JWTConsumerRemoval]), "JWTConsumerRemoval")
  val emailNotificationUserProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.EmailNotificationUserProcessor]), "emailNotificationUserProcessor")
  val updatePasswordProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.UpdatePasswordProcessor]), "updatePasswordProcessor")
  val forgotPasswordProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.ForgotPasswordProcessor]), "forgotPasswordProcessor")
  val deleteUserProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.DeleteUserProcessor]), "deleteUserProcessor")
  val FriendInvitationDispatcher = system.actorOf(RoundRobinPool(20).props(Props[dispatcher.FriendInvitationDispatcher]), "FriendInvitationDispatcher")
  val GetUserDetailsProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.GetUserDetailsProcessor]), "GetUserDetailsProcessor")
  val resendVerifEmailProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.ResendVerifEmailProcessor]), "resendVerifEmailProcessor")
  //val BirthdayProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.BirthdayProcessor]), "BirthdayProcessor")
  val GetFriendBirthdayDetailsProcessor = system.actorOf(RoundRobinPool(20).props(Props[processor.GetFriendBirthdayDetailsProcessor]), "GetFriendBirthdayDetailsProcessor")
  import system.dispatcher
  //system.scheduler.scheduleOnce(20 seconds , BirthdayProcessor ,"schedule")

  Http().bindAndHandle(RouteUtils.logRoute, "0.0.0.0", port)

  consoleLog("INFO",
             s"User server started! Access url: https://$hostName:$port/")
}

