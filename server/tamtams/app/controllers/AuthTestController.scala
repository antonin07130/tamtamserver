package controllers

import javax.inject._

import play.api.mvc._

import scala.concurrent.Future

/**
  * This controller is used to test and implement authentication.
  * It is no meant to stay in the project
  * TODO : remove this controller once authentication is working
  */
@Singleton
class AuthTestController @Inject() extends Controller {

  def authAction() = Action{
    Ok("voila Auth")
  }

  def asyncAuthAction() = Action.async {
    request => {
      Future.successful(Ok("voila asyncAuth"))
    }
  }

  def noAuthAction() = Action {
    request => {
      Ok("voila noAuth")
    }
  }

}
