package com.mj.users.model

import java.util.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

//login api user request
case class LoginDto(email: String, password: String, user_agent: Option[String], location: Option[Location])

//logout api user request
case class UidDto(uid: String)

//RegisterDto api user request
case class RegisterDto(email: String, nickname: String, password: String, repassword: String,
                       gender: Int, firstname: String, lastname: String, contact_info: Option[ContactInfo],
                       location: Option[Location],connections :  Option[List[ConnectionsDto]],
                       user_agent: Option[String])

case class loginHistory(memberID: String, user_agent: Option[String], location: Option[Location])

//RegisterDto api user response
case class RegisterDtoResponse(memberID: String, firstname: String, lastname: String, email: String, avatar: String , created_date : String)

//RegisterDto DB user case class
case class DBRegisterDto(var _id: String, status : String ,avatar: String, created_date: Option[String], updated_date: Option[String],
                         registerDto: RegisterDto,
                         intro: Option[Intro], /*experience collection*/
                         experience: Option[userExperience], /*experience collection*/
                         education: Option[userEducation], /*education collection*/
                         Interest: Option[List[String]], /*interest details*/
                         userIP: Option[String], country: Option[String], interest_on_colony: Option[String], employmentStatus: Option[String], interest: Option[List[String]]/*extra fields from second step page*/
                         , secondSignup_flag: Option[Boolean] = Some(false), email_verification_flag: Option[Boolean] = Some(false),connections_flag : Option[Boolean]= Some(false) , /*user prfile flags*/
                         lastLogin: Long = 0, loginCount: Int = 0, sessionsStatus: List[SessionStatus] = List(), dateline: Long = System.currentTimeMillis()
                        )

/*default value*/

case class Location(city: Option[String], state: Option[String], country: Option[String], countryCode: Option[String], lat: Option[Double], lon: Option[Double], ip: Option[String], region: Option[String], regionName: Option[String], timezone: Option[String], zip: Option[String])
case class WebProfile(name: String, link:String)
case class ContactInfo(address: String, city: String, state: String, country: String, email: Option[String], mobile_phone: Option[String], birth_day: Option[Int], birth_month: Option[Int], birth_year: Option[Int], web_profile: Option[List[WebProfile]])


//Experience Collection

case class Experience(expID: String, status : String , memberID: String, position: Option[String], career_level: Option[String], description: Option[String], employer: Option[String],
                      start_month: Option[String],
                      start_year: Option[String], end_month: Option[String], end_year: Option[String], created_date: Option[String], updated_date: Option[String],
                      current: Option[Boolean], industry: Option[String]
                     )
case class Intro(current_position: Option[String], current_employer: Option[String], school_name: Option[String], field_of_study: Option[String], degree: Option[String])

case class userExperience(headline: String, position: Option[String], career_level: Option[String], description: Option[String], employer: Option[String], start_month: Option[String],
                          start_year: Option[String], end_month: Option[String], end_year: Option[String], current: Option[Boolean],
                          industry: Option[String])

//Education Collection
case class Education(eduID: String, status : String , memberID: String, school_name: Option[String], field_of_study: Option[String], degree: Option[String],
                     start_year: Option[String], end_year: Option[String], activities: Option[String], created_date: Option[String], updated_date: Option[String])

case class userEducation(school_name: Option[String], field_of_study: Option[String], degree: Option[String],
                         start_year: Option[String], end_year: Option[String], activities: Option[String])


//SecondSignupStep api user response
case class SecondSignupStep(memberID: String, country: String, employmentStatus: String,
                            employer: Option[String], position: Option[String], career_level: Option[String], description: Option[String], industry: Option[String], degree: Option[String],
                            school_name: Option[String], field_of_study: Option[String], activities: Option[String], current: Boolean, interest_on_colony: Option[String], userIP: Option[String],
                            updated_date: Option[String], start_month: Option[String], start_year: Option[String], end_month: Option[String], end_year: Option[String], connections: Option[List[ConnectionsDto]],interest: Option[String])


case class SessionStatus(sessionid: String, newCount: Int)

//Online Collection
case class Online(var _id: String, uid: String, dateline: Date = new Date())

//Response format for all apis
case class responseMessage(uid: String, errmsg: String, successmsg: String)

//Interest collection for users
case class Interest(memberID: String, interest: Option[String])

//Update Personal Info
case class PersonalInfo(memberID: String, firstname:String, lastname:String, summary:Option[String], contact_info: ContactInfo)

case class TokenID(id: String)

case class TokenDetails(rsa_public_key: String, consumer: Option[TokenID], created_at: Long, id: String, key: String, secret: String, algorithm: Option[String])

case class ConsumerDetails(userName: String, created_at: Long, id: String)

case class listConsumerDetails(next: String, data: List[TokenDetails])

case class Consumer(username: String)


case class UpdatePasswordDto(email: String, password: String, repassword: String,
                             location: Option[Location],
                             user_agent: Option[String])


case class ForgotPasswordDto(email: String)

case class ForgotPasswordKafkaMsg(email: String , firstName : String , lastName : String )

case class ConnectionsDto (memberID : String , conn_type : String , status : String )

case class MultipleInvitation(memberID: String, connections: Option[List[ConnectionsDto]])

case class FriendBirthdayDetails(memberID : String ,  birth_day: String, birth_month: String, birth_year: String)

case class BirthdayDetails ( memberID : String , friendDetails : List[FriendBirthdayDetails])

case class ListBirthdayDetails ( birthdayDetails : List[BirthdayDetails])

object JsonRepo extends DefaultJsonProtocol with SprayJsonSupport {

  implicit val forgotPWKafkaMsgFormats: RootJsonFormat[ForgotPasswordKafkaMsg] = jsonFormat3(ForgotPasswordKafkaMsg)
  implicit val connectionsDtoFormats: RootJsonFormat[ConnectionsDto] = jsonFormat3(ConnectionsDto)
  implicit val multipleInvitationFormats: RootJsonFormat[MultipleInvitation] = jsonFormat2(MultipleInvitation)
  implicit val friendBirthdayDetailsFormats: RootJsonFormat[FriendBirthdayDetails] = jsonFormat4(FriendBirthdayDetails)
  implicit val birthdayDetailsFormats: RootJsonFormat[BirthdayDetails] = jsonFormat2(BirthdayDetails)

  implicit val locationFormats: RootJsonFormat[Location] = jsonFormat11(Location)
  implicit val loginDtoFormats: RootJsonFormat[LoginDto] = jsonFormat4(LoginDto)
  implicit val uidDtoFormats: RootJsonFormat[UidDto] = jsonFormat1(UidDto)


  implicit val webProfileFormats: RootJsonFormat[WebProfile] = jsonFormat2(WebProfile)
  implicit val contactInfoFormats: RootJsonFormat[ContactInfo] = jsonFormat10(ContactInfo)
  implicit val registerDtoFormats: RootJsonFormat[RegisterDto] = jsonFormat11(RegisterDto)
  implicit val errorMessageDtoFormats: RootJsonFormat[responseMessage] = jsonFormat3(responseMessage)
  implicit val registerDtoResponseDtoFormats: RootJsonFormat[RegisterDtoResponse] = jsonFormat6(RegisterDtoResponse)
  implicit val secondSignupStepsFormats: RootJsonFormat[SecondSignupStep] = jsonFormat22(SecondSignupStep)
  implicit val interestFormats: RootJsonFormat[Interest] = jsonFormat2(Interest)
  implicit val personalInfoFormats: RootJsonFormat[PersonalInfo] = jsonFormat5(PersonalInfo)
  implicit val tokenIDFormats: RootJsonFormat[TokenID] = jsonFormat1(TokenID)
  implicit val tokenDetailsFormats: RootJsonFormat[TokenDetails] = jsonFormat7(TokenDetails)
  implicit val consumerFormats: RootJsonFormat[Consumer] = jsonFormat1(Consumer)
  implicit val consumerDetailsFormats: RootJsonFormat[ConsumerDetails] = jsonFormat3(ConsumerDetails)
  implicit val consumerDetailsListFormats: RootJsonFormat[listConsumerDetails] = jsonFormat2(listConsumerDetails)
  implicit val updatePasswordDtoFormats: RootJsonFormat[UpdatePasswordDto] = jsonFormat5(UpdatePasswordDto)
  implicit val forgotPasswordDtoFormats: RootJsonFormat[ForgotPasswordDto] = jsonFormat1(ForgotPasswordDto)
  implicit val userExperienceFormats: RootJsonFormat[userExperience] = jsonFormat11(userExperience)
  implicit val userEducationFormats: RootJsonFormat[userEducation] = jsonFormat6(userEducation)
  implicit val userIntroFormats: RootJsonFormat[Intro] = jsonFormat5(Intro)
  implicit val sessionStatusFormats: RootJsonFormat[SessionStatus] = jsonFormat2(SessionStatus)
  implicit val dBRegisterDtoFormats: RootJsonFormat[DBRegisterDto] = jsonFormat22(DBRegisterDto)
  implicit val experinceFormats: RootJsonFormat[Experience] = jsonFormat15(Experience)

}

