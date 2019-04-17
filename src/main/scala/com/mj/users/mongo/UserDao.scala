package com.mj.users.mongo

import java.io.File
import java.util.Date

import com.mj.users.model._
import com.mj.users.mongo.MongoConnector._
import com.mj.users.tools.CommonUtils._
import org.apache.commons.io.FileUtils
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._

import scala.concurrent.Future
import com.mj.users.config.Application._
import org.joda.time.DateTime
object UserDao {

  val usersCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("users"))
  val onlinesCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("onlines"))
  val experienceCollection: Future[BSONCollection] = db1.map(_.collection[BSONCollection]("experience"))
  val eductionCollection: Future[BSONCollection] = db1.map(_.collection[BSONCollection]("education"))
  val loginHistoryCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("loginHistory"))
  val birthdayCollection: Future[BSONCollection] = db.map(_.collection[BSONCollection]("birthday"))
  implicit def sessionStatusHandler = Macros.handler[SessionStatus]

  implicit def onlineHandler = Macros.handler[Online]

  implicit def locationHandler = Macros.handler[Location]

  implicit def contactStatusHandler = Macros.handler[ContactInfo]

  implicit def userExperienceHandler = Macros.handler[userExperience]

  implicit def userEducationHandler = Macros.handler[userEducation]

  implicit def connectionsDtoHandler = Macros.handler[ConnectionsDto]

  implicit def registerHandler = Macros.handler[RegisterDto]

  implicit def experienceHandler = Macros.handler[Experience]

  implicit def educationeHandler = Macros.handler[Education]

  implicit def friendBirthdayDetailsHandler = Macros.handler[FriendBirthdayDetails]

  implicit def birthdayDetailsHandler = Macros.handler[BirthdayDetails]

  implicit def ListBirthdayDetailsHandler = Macros.handler[ListBirthdayDetails]

  implicit def dbRegisterHandler = Macros.handler[DBRegisterDto]

  implicit def loginHistoryRegisterHandler = Macros.handler[loginHistory]

  val defaultAvatar = getDefaultAvatar

  //insert user Details
  def insertUserDetails(userRequest: RegisterDto): Future[RegisterDtoResponse] = {
    for {
      avatar <- defaultAvatar.map { avatarMap =>
        userRequest.gender match {
          case 1 => avatarMap("boy")
          case 2 => avatarMap("girl")
          case _ => avatarMap("unknown")
        }
      }
      prepareUseRequest <- Future {
        userRequest.copy(password = sha1(userRequest.password), repassword = sha1(userRequest.password))
      }
      userData <- Future {
        DBRegisterDto(BSONObjectID.generate().stringify, active ,avatar,
          Some(DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")),
          None,
          prepareUseRequest, None, None, None, None, None, None, None,None,None)
      }
      response <- insert[DBRegisterDto](usersCollection, userData).map {
        resp => RegisterDtoResponse(resp._id, resp.registerDto.firstname, resp.registerDto.lastname, resp.registerDto.email, resp.avatar, DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"))
      }


    }
      yield (response)
  }

  def getUserDetails(userRequest: RegisterDto): Future[Option[DBRegisterDto]] = {
    search[DBRegisterDto](usersCollection,
      document("registerDto.email" -> userRequest.email ,"status" -> active))
  }

  def getUserDetailsById(id: String): Future[Option[DBRegisterDto]] = {
    search[DBRegisterDto](usersCollection,
      document("_id" -> id  ,"status" -> active))

  }

  def getAllUsers: Future[List[DBRegisterDto]] = {
    searchAll[DBRegisterDto](usersCollection,
      document("status" -> "active"/*active*/ /*,"_id" -> "5cb1d4070b00008300cbe1ea"*/) ,100)

  }

  def getFiendBirthdayDetails(memberID : String) = {
    search[ListBirthdayDetails](birthdayCollection,
        document("birthdayDetails" -> document( "$elemMatch"->  document( "memberID"-> memberID))) )

  }

  def updateUserDetails(secondStepRequest: SecondSignupStep ,userObj : Option[DBRegisterDto]): Future[String] = {


    val interests = if(secondStepRequest.interest.isDefined) Some(secondStepRequest.interest.get.split(",").toList) else None
    val conn = if(secondStepRequest.connections.isDefined) Some(secondStepRequest.connections.get) else None
    val user = if (secondStepRequest.employmentStatus.toInt > 5) {
      userObj.get.copy(
        education =Some(userEducation(secondStepRequest.school_name,
          secondStepRequest.field_of_study, secondStepRequest.degree, secondStepRequest.start_year, secondStepRequest.end_year, secondStepRequest.activities)),
        interest_on_colony =secondStepRequest.interest_on_colony,
        country = Some(secondStepRequest.country),
        userIP = secondStepRequest.userIP,
        employmentStatus = Some(secondStepRequest.employmentStatus),
        secondSignup_flag = Some(true),
        updated_date = Some(DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")),
        registerDto = userObj.get.registerDto.copy(connections = conn),
        interest = interests
      )

    } else {

      userObj.get.copy(
        experience =Some(userExperience(secondStepRequest.position, secondStepRequest.career_level,
          secondStepRequest.description, secondStepRequest.employer, secondStepRequest.start_month, secondStepRequest.start_year,
          secondStepRequest.end_month, secondStepRequest.end_year, Some(secondStepRequest.current), secondStepRequest.industry)),
        interest_on_colony =secondStepRequest.interest_on_colony,
        country = Some(secondStepRequest.country),
        userIP = secondStepRequest.userIP,
        employmentStatus = Some(secondStepRequest.employmentStatus),
        secondSignup_flag = Some(true),
        updated_date = Some(DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")),
        registerDto = userObj.get.registerDto.copy(connections = conn),
        interest = interests
      )


    }

    updateDetails(usersCollection, {
      BSONDocument("_id" -> secondStepRequest.memberID,"status" -> active)
    }, user).map(resp => resp)

  }

  def insertLoginHistory(memberId: String, user_Agent: Option[String], location: Option[Location]) = {
    for {

      userData <- Future {
        loginHistory(memberId, user_Agent, location)
      }
      response <- insert[loginHistory](loginHistoryCollection, userData)


    }
      yield (response)
  }

  def insertExperienceDetails(secondStepRequest: SecondSignupStep) = {
    if (secondStepRequest.employmentStatus.toInt > 5) {
      Future {
        Education(
          BSONObjectID.generate().stringify,
          active ,
          secondStepRequest.memberID,
          secondStepRequest.school_name,
          secondStepRequest.field_of_study,
          secondStepRequest.degree,
          secondStepRequest.start_year,
          secondStepRequest.end_year,
          secondStepRequest.activities,
          None,
          None
        )
      }.flatMap(eductionData => insert[Education](eductionCollection, eductionData).map(response => response))

    } else {
      Future {
        Experience(
          BSONObjectID.generate().stringify,
          active,
          secondStepRequest.memberID,
          secondStepRequest.position,
          secondStepRequest.career_level,
          secondStepRequest.description,
          secondStepRequest.employer,
          secondStepRequest.start_month,
          secondStepRequest.start_year
          , secondStepRequest.end_month, secondStepRequest.end_year, None, None, Some(secondStepRequest.current), secondStepRequest.industry
        )
      }.flatMap(expirenceData => insert[Experience](experienceCollection, expirenceData).map(response => response))
    }
  }


  def updateUserInterestDetails(interestReq: Interest): Future[String] = {

    update(usersCollection, {
      BSONDocument("_id" -> interestReq.memberID)
    }, {
      BSONDocument("$set" -> BSONDocument("interest" -> interestReq.interest.get.split(","),
          "updated_date" -> Some(DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"))))
    }).map(resp => resp)

  }

  def insertBirthdayDetails(birthdayDetails: ListBirthdayDetails): Future[String] = {

    insert[ListBirthdayDetails](birthdayCollection, birthdayDetails).map(resp => "Successfully inserted into Data store")

  }


  def updatePasswordDetails(updatePasswordDto: UpdatePasswordDto): Future[String] = {

    update(usersCollection, {
      BSONDocument("registerDto.email" -> updatePasswordDto.email)
    }, {
      BSONDocument("$set" -> BSONDocument(
        "user_agent" -> updatePasswordDto.user_agent,
        "registerDto.location" -> updatePasswordDto.location,
        "password" -> sha1(updatePasswordDto.password), "repassword" -> sha1(updatePasswordDto.password),
        "updated_date" -> Some(DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"))))
    }).map(resp => resp)

  }


  def forgotPasswordDetails(forgotPasswordDto: ForgotPasswordDto):  Future[Option[DBRegisterDto]]  = {

    search[DBRegisterDto](usersCollection,
      document("registerDto.email" -> forgotPasswordDto.email , "status" -> active))

  }

  def updateUserInfoDetails(personalInfo: PersonalInfo): Future[String] = {

    update(usersCollection, {
      BSONDocument("_id" -> personalInfo.memberID)
    }, {
      BSONDocument(
        "$set" -> BSONDocument("registerDto.contact_info" -> personalInfo.contact_info,"updated_date" -> Some(DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"))
        ))
    }).map(resp => resp)

  }

  def updateUserStatusFlag(memberID : String): Future[String] = {

    update(usersCollection, {
      BSONDocument("_id" -> memberID)
    }, {
      BSONDocument(
        "$set" -> BSONDocument("status" -> deleted ,
          "updated_date" -> Some(DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ"))
      ))
    }).map(resp => resp)

  }

  //update user online status
  def updateOnline(uid: String) = {
    val selector = document("uid" -> uid)
    for {
      online <- search[Online](onlinesCollection, selector)
      details <- {
        online match {
          case None => insert[Online](onlinesCollection, Online(BSONObjectID.generate().stringify, uid, new Date()))
          case _ => update(onlinesCollection, selector, document("$set" -> document("dateline" -> new Date())))
        }
      }
    } yield ()
  }

  //when user login, update the loginCount and online info
  def loginUpdate(uid: String, login: LoginDto): Future[String] = {
    for {
      onlineResult <- updateOnline(uid)
      loginResult <- {
        val selector = document("_id" -> uid)
        val updateDoc = document("$set" -> document(
          "user_agent" -> login.user_agent,
          "registerDto.location" -> login.location),
          "$inc" -> document("loginCount" -> 1)
        )
        update(usersCollection, selector, updateDoc)
      }
    } yield {
      loginResult
    }
  }

  def emailVerification(memberID: String , userObj : DBRegisterDto): Future[String] = {
    println("memberID:" + memberID)

    updateDetails(usersCollection, {
      BSONDocument("_id" -> memberID,"status" -> active)
    }, userObj.copy(email_verification_flag = Some(true)  , updated_date = Some(DateTime.now.toString("yyyy-MM-dd'T'HH:mm:ssZ")))).map(resp => resp)

  }

  def getDefaultAvatar: Future[Map[String, String]] = {
    for {
      (idBoy, fileNameBoy, fileTypeBoy, fileSizeBoy, fileMetaDataBoy, errmsgBoy) <- getGridFileMeta(
        document("metadata" -> document("avatar" -> "boy")))
      bsidBoy <- {
        if (fileNameBoy == "") {
          val bytes =
            FileUtils.readFileToByteArray(
              new File("src/main/resources/avatar/boy.jpg"))
          saveGridFile(bytes,
            fileName = "boy.jpg",
            contentType = "image/jpeg",
            metaData = document("avatar" -> "boy")).map(_._1)
        } else {
          Future(idBoy)
        }
      }

      (idGirl,
      fileNameGirl,
      fileTypeGirl,
      fileSizeGirl,
      fileMetaDataGirl,
      errmsgGirl) <- getGridFileMeta(
        document("metadata" -> document("avatar" -> "girl")))
      bsidGirl <- {
        if (fileNameGirl == "") {
          val bytes = FileUtils.readFileToByteArray(
            new File("src/main/resources/avatar/girl.jpg"))
          saveGridFile(bytes,
            fileName = "girl.jpg",
            contentType = "image/jpeg",
            metaData = document("avatar" -> "girl")).map(_._1)
        } else {
          Future(idGirl)
        }
      }

      (idUnknown,
      fileNameUnknown,
      fileTypeUnknown,
      fileSizeUnknown,
      fileMetaDataUnknown,
      errmsgUnknown) <- getGridFileMeta(
        document("metadata" -> document("avatar" -> "unknown")))
      bsidUnknown <- {
        if (fileNameUnknown == "") {
          val bytes = FileUtils.readFileToByteArray(
            new File("src/main/resources/avatar/unknown.jpg"))
          saveGridFile(bytes,
            fileName = "unknown.jpg",
            contentType = "image/jpeg",
            metaData = document("avatar" -> "unknown")).map(_._1)
        } else {
          Future(idUnknown)
        }
      }
    } yield {
      var idBoyStr = ""
      var idGirlStr = ""
      var idUnknownStr = ""
      bsidBoy match {
        case bsid: BSONObjectID =>
          idBoyStr = bsid.stringify
        case _ =>
      }
      bsidGirl match {
        case bsid: BSONObjectID =>
          idGirlStr = bsid.stringify
        case _ =>
      }
      bsidUnknown match {
        case bsid: BSONObjectID =>
          idUnknownStr = bsid.stringify
        case _ =>
      }
      Map(
        "boy" -> idBoyStr,
        "girl" -> idGirlStr,
        "unknow" -> idUnknownStr
      )
    }
  }

}
