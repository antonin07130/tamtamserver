package controllers

import javax.inject._

import akka.util.ByteString
import play.api._
import play.api.mvc._
import play.api.http.HttpEntity
import models._
import play.api.Logger
import play.api.libs.json._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  *
  * Not much for tamtams as of today
  */
@Singleton
class HomeController @Inject() extends Controller {

  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index = TODO

  // todo : get things near with database near functions
  def getThingsNear(lat: Double, lon: Double) = TODO

  def throwExceptionAction() = Action{
    if (true)
      throw new IllegalStateException("NONONONO !!!!")
    Ok("good")
  }


}
