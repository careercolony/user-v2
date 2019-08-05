package com.mj.users

import config.Application._
import model.{ResendEmailInfo}
import org.matthicks.mailgun._

import scala.concurrent._
import scala.concurrent.duration._
import java.io._

import com.mashape.unirest.http.Unirest



object MailGun {

    def resendVerificationEmail(message: ResendEmailInfo) = {
        val request = Unirest.post("https://api.mailgun.net/v3/" + domainName + "/messages")
        .basicAuth("api", apiKey)
        //.field("content-type",  "multipart/form-data;")
        .field("from", ""+ fromEmailName +"  "+ fromEmailAddress +"")
        .field("to", ""+ message.email +"") //message.inviteeEmail)
        .field("subject",  RegisterSubject)
        .field("template", "signup")
        .field("h:X-Mailgun-Variables", "{\"firstname\":\" " + message.firstname + " \", \"lastname\":\" " + message.lastname + " \",  \"memberID\":\""+ message.memberID + " \"}")
        .asJson()
    }
/**
  def sendInvitationMail(message: ConnectionInvitationDto) = {
    println("parsed message from topic invitation:" + message)
    
    val employerVal: String = message.employer match { case None => "" case Some(str) => str }
    val positionVal: String = message.position match { case None => "" case Some(str) => str }
    val stateVal: String = message.state match { case None => "" case Some(str) => str }
    val countryVal: String = message.country match { case None => "" case Some(str) => str }
    
    val request = Unirest.post("https://api.mailgun.net/v3/" + domainName + "/messages")
      .basicAuth("api", apiKey)
      //.field("content-type",  "multipart/form-data;")
      .field("from", ""+ message.firstname +" "+ message.lastname +"  "+ fromEmailAddressInvite +"")
      .field("to", ""+ message.inviteeEmail +"") //message.inviteeEmail)
      .field("subject",  ""+ message.inviteeName +", please add me to your colony")
      .field("template", "connection_invitation")
      .field("h:X-Mailgun-Variables", "{\"inviteeName\":\" " + message.inviteeName +" \", \"firstname\":\" " + message.firstname + " \", \"lastname\":\" " + message.lastname + " \", \"avatar\":\" " + message.avatar + " \", \"email\":\" " + message.email + " \", \"state\":\" " + stateVal + " \", \"country\":\" " + countryVal + " \", \"employer\":\" " + employerVal + " \", \"position\":\" " + positionVal + " \", \"inviteeEmail\":\" " + message.inviteeEmail + " \"}")
      .asJson()

    //println( request.getBody())
    
  }
*/

  

}

