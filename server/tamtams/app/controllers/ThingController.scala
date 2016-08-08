package controllers

import javax.inject._

import akka.util.ByteString
import logic._
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.mvc._
import logic.JsonConversion._

/**
  * This controller creates Actions to handle HTTP requests
  * related to [[Thing]]
  */
@Singleton
class ThingController @Inject() extends Controller {


// TODO : handle bad conversions without exceptions
  /**
    * As of today this action can take a Json as a parameter
    * and parse it into a [[Thing]] object.
    * It throws exceptions when it is unhappy.
    * @param thingId Id of the thing to be created
    *                This Id is also part of the Json (Thing/id)
    * @return
    */
  def putThing(thingId: String) = Action(parse.json) {request =>
    Logger.info(request.body.toString())
    Logger.info(request.body.as[Thing].toString)
    Ok("gogoog")
  }

}
