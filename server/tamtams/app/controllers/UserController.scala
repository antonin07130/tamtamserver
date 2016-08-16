package controllers

import javax.inject._
import akka.util.ByteString
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.mvc._
import logic.JsonConversion._
import logic.User
import logic.UsersGenerator

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class UserController @Inject() extends Controller {


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
  // todo : divide this big block into functions
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

}
