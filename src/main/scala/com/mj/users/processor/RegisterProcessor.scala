package com.mj.users.processor

import java.util.concurrent.TimeUnit
import org.joda.time.DateTime
import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{RegisterDto, ResendEmailInfo, responseMessage}
import com.mj.users.mongo.Neo4jConnector.connectNeo4j
import com.mj.users.mongo.UserDao._
import com.mj.users.MailGun._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global

class RegisterProcessor extends Actor with MessageConfig{

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  val wfsRescheduleWOLog = LoggerFactory.getLogger(this.getClass.getName)

  def receive = {

    case (resgisterDto: RegisterDto) => {
      val origin = sender()
      val result = getUserDetails(resgisterDto)
        .flatMap(user =>
          user match {
            case Some(user) => {
              throw new Exception(userExistMsg)
            }
            case None => {
               insertUserDetails(resgisterDto)
            }
          }
        ).map(response =>{
        insertLoginHistory(response.memberID,resgisterDto.user_agent,resgisterDto.location, DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"))
      
      val verEmail = verificationEmail(ResendEmailInfo(response.memberID, response.email, response.firstname, response.lastname))
      val script = s"CREATE (s:users {memberID:'${response.memberID}', firstname:'${ response.firstname }', lastname:'${ response.lastname }', email:'${ resgisterDto.email }', password:'${resgisterDto.password}' , avatar:'${response.avatar}', signupdate: TIMESTAMP()}) "

      connectNeo4j(script).map(resp => resp match {
        case count if count > 0 => origin ! response
        case 0 => origin ! responseMessage("", s"insert record email : ${response.firstname}", "")
      })
    })

      result.recover {
        case e: Throwable => {
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
