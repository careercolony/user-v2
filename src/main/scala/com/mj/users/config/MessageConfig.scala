package com.mj.users.config

import com.typesafe.config.ConfigFactory

/**
  * Created by rmanas001c on 1/6/2018
  */
trait MessageConfig {
  val conf = ConfigFactory.load("messages.conf")

  /* Success Mesages*/
  val loginSuccess = conf.getString("successMessages.loginSuccess")
  val logoutSuceess = conf.getString("successMessages.logoutSuceess")
  val emailSuccess = conf.getString("successMessages.emailSuccess")
  val secondSignupSuccess = conf.getString("successMessages.secondSignupSuccess")
  val updateInterestSuccess = conf.getString("successMessages.updateInterestSuccess")
  val updatePasswordSuccess = conf.getString("successMessages.updatePasswordSuccess")
  val forgotPasswordSuccess = conf.getString("successMessages.forgotPasswordSuccess")
  val resendVerificationEmailSuccess = conf.getString("successMessages.resendVerificationEmailSuccess")


  /* Error Codes & descriptions*/

  val loginFailed = conf.getString("errorMessages.loginFailed")
  val userExistMsg = conf.getString("errorMessages.userExistMsg")
  val secondSignupFailed = conf.getString("errorMessages.secondSignupFailed")
  val updateInterestFailed = conf.getString("errorMessages.updateInterestFailed")
  val forgotPasswordFailed = conf.getString("errorMessages.forgotPasswordFailed")
  val resendVerificationEmailFailed = conf.getString("errorMessages.loginFailed")
  


}
