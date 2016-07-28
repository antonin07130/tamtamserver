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
    val rocky = User(1,"Rocky",38)
    val steve = User(2,"Steve",50)
    val melinda = User(3,"Melinda",38)

    val usersList : Seq[User] = Seq(rocky,steve,melinda)
    Logger.info(s"tamtams : users created : $usersList.")



    case class Location(lat: Double, long: Double)
    case class Resident(name: String, age: Int, role: Option[String])
    case class Place(name: String, location: Location, residents: Seq[Resident])

    import play.api.libs.json._

    implicit val userWrites = new Writes[User] {
      def writes(user: User) = Json.obj(
        "id" -> user.id,
        "name" -> user.name,
        "age" -> user.age
      )
    }


    val jsonUsersList = Json.toJson(usersList)


    Logger.info(s"tamtams : users converted : $jsonUsersList.")


    Result(
      header = ResponseHeader(200, Map.empty),
      body = HttpEntity.Strict(ByteString(jsonUsersList.toString()), Some("application/json"))
    )
  }


  
  def getThings(lat: Double, lon : Double) = TODO

}
