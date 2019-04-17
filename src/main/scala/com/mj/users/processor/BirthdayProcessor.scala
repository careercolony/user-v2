package com.mj.users.processor

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.util.Timeout
import com.mj.users.config.MessageConfig
import com.mj.users.model.{BirthdayDetails, FriendBirthdayDetails, ListBirthdayDetails, responseMessage}
import com.mj.users.mongo.MongoConnector.remove
import com.mj.users.mongo.UserDao.{birthdayCollection, getAllUsers, insertBirthdayDetails}
import org.joda.time.{DateTime, Days}
import org.slf4j.LoggerFactory
import reactivemongo.bson._

import scala.concurrent.ExecutionContext.Implicits.global

class BirthdayProcessor extends Actor with MessageConfig {

  implicit val timeout = Timeout(500, TimeUnit.SECONDS)
  val getUserDetailsProcessor = LoggerFactory.getLogger(this.getClass.getName)

  def receive = {

    case _ => {
      println("inside birthday scheduler")

      val origin = sender()
      val result = remove(birthdayCollection, document()).map(resp => getAllUsers
        .map(userDetails => {
          val allUserDbDetails = {
            userDetails.map(userObj => {
              println("user Object :" + userObj)
              if (userObj.registerDto.connections.isDefined) {
                val friendBirthdayDetails = userObj.registerDto.connections.get.filter(p => p.status == "active").map(
                  activeUser => {
                    val contactInfo = userDetails.filter(p => p._id == activeUser.memberID).head.registerDto.contact_info
                    println("contact Info" + contactInfo)
                    val birthdayDetails = if (contactInfo.isDefined) {
                      if (contactInfo.get.birth_month.isDefined && contactInfo.get.birth_day.isDefined && contactInfo.get.birth_year.isDefined) {
                        val birthDate = new DateTime(DateTime.now().getYear, contactInfo.get.birth_month.get, contactInfo.get.birth_day.get, 0, 0, 0, 0)
                        val now = DateTime.now()
                        val period = Days.daysBetween(birthDate, now)
                        println("period" + period)
                        if (period.getDays < 3)
                          FriendBirthdayDetails(activeUser.memberID, contactInfo.get.birth_day.get.toString, contactInfo.get.birth_month.get.toString, contactInfo.get.birth_year.get.toString)
                        else
                          FriendBirthdayDetails("", "", "", "")
                      } else
                        FriendBirthdayDetails("", "", "", "")
                    } else
                      FriendBirthdayDetails("", "", "", "")
                    println("birthday Details :" + birthdayDetails)
                    birthdayDetails
                  }
                ).filter(p => p.memberID != "")

                BirthdayDetails(userObj._id, friendBirthdayDetails.filter(p => p.memberID != ""))
              } else
                BirthdayDetails("", Nil)
            }
            )
          }
          allUserDbDetails
        }
        ).map(allUserDbDetails => {
        println("all User Db Details:" + allUserDbDetails)
        insertBirthdayDetails(ListBirthdayDetails(allUserDbDetails.filter(p => p.memberID != null && p.friendDetails.size > 0))).map(resp => origin ! resp)
      }
      )
      )
      result.recover {
        case e: Throwable => {
          println("e:" + e.getMessage)
          origin ! responseMessage("", e.getMessage, "")
        }
      }
    }
  }
}
