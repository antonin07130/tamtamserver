package controllers

import javax.inject._
import akka.util.ByteString
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.mvc._
import logic.JsonConversion._
import logic.Thing
import logic.ThingsGenerator


/**
  * This controller creates Actions to handle HTTP requests
  * related to [[Thing]]
  */
@Singleton
class ThingController @Inject() extends Controller {


// TODO : test and return URI with created status
  /**
    * As of today this action can take a Json as a parameter
    * and parse it into a [[Thing]] object.
    * It answers with a bad request when unhappy
    * @param thingId Id of the thing to be created
    *                This Id is also part of the Json (Thing/id)
    * @return
    */
  def putThing(thingId: String) = Action(parse.json[Thing]) {request =>
    Logger.info(request.body.toString())
    Created
  }


  // todo : put this code in a function taking a list of anything having an id and returning json or notfound
  def getThing(thingId: String) = Action {
    val foundThing: Option[Thing] = ThingsGenerator.thingSeq.find(thing => thing.id == thingId)
    if (foundThing.isDefined) {
      val jsonThing = Json.toJson(foundThing.get)
      val jsonString = jsonThing.toString
      Logger.info(s"tamtams : Thing -> JSON : ${jsonString} ")
      Result(
        header = ResponseHeader(OK, Map.empty),
        body = HttpEntity.Strict(ByteString(jsonString), Some("application/json"))
      )
    } else {
      Logger.info(s"tamtams : ${thingId} Not found ")
      NotFound
    }
  }

  // todo : get things near with database near functions
  def getThingsNear(lat: Double, lon: Double) = TODO

}
