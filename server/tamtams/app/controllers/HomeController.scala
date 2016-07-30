package controllers

import javax.inject._

import akka.util.ByteString
import play.api._
import play.api.mvc._
import play.api.http.HttpEntity
import logic._
import play.api.Logger
import play.api.libs.json._
import logic.JsonConversion._

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
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

  /**
    * Create an Action to return a json object response
    * containing a list of List of [[User]].
    */
  def getUsers = Action {
    // import predefined list of users and convert it to Json
    val jsonUsersList = Json.toJson(UsersGenerator.userSeq)
    Logger.info(s"tamtams : users converted : $jsonUsersList.")

    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Strict(ByteString(jsonUsersList.toString()), Some("application/json"))
    )
  }

  // todo : returns a specific user according to its id
  def getUser(userId: String) = Action {
    val foundUser: Option[User] = UsersGenerator.userSeq.find(user => user.id == userId)
    if (foundUser.isDefined) {
      val jsonUser = Json.toJson(foundUser.get)
      val jsonString = jsonUser.toString
      Logger.info(s"tamtams : User -> JSON : ${jsonString} ")
      Result(
        header = ResponseHeader(OK, Map.empty),
        body = HttpEntity.Strict(ByteString(jsonString), Some("application/json"))
      )
    } else {
      Logger.info(s"tamtams : ${userId} Not found ")
      NotFound
    }
  }

  // todo : getThings
  def getThings = TODO

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
