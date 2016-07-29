package controllers

import javax.inject._

import akka.util.ByteString
import play.api._
import play.api.mvc._
import play.api.http.HttpEntity


import logic._
import play.api.Logger
import play.api.libs.json._



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
    * Create an Action to return a json object containing a list of users.
    */
  def getUsers = returnUsersAction

  //todo : cleanup this mess and make it pretty

  def returnUsersAction = Action {
    import play.api.libs.json._


    implicit val userWrites = new Writes[User] {
      def writes(user: User) = Json.obj(
        "id" -> user.id,
        "name" -> user.name,
        "age" -> user.age
      )
    }

    val jsonUsersList = Json.toJson(UsersGenerator.usersList)

    Logger.info(s"tamtams : users converted : $jsonUsersList.")

    val testObjList = ThingsGenerator.testThingString1
    Logger.info(s"tamtams : things used once : $testObjList.")

    val testThingJson = Json.parse(ThingsGenerator.testThingString1)
    val testThing : Thing = Json.fromJson[Thing](testThingJson )
    Logger.info(s"tamtams : things used once : $testThing.")

    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Strict(ByteString(jsonUsersList.toString()), Some("application/json"))
    )
  }


  
  def getThings(lat: Double, lon : Double) = TODO

}
