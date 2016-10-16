package controllers

import javax.inject._

import com.typesafe.config.ConfigFactory
import database.UserRepo
import play.api.Logger
import play.api.mvc._
import models.User
import utils.UserJsonConversion._
import play.api.libs.json._
import play.libs.Json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.commands._
import reactivemongo.api.collections._
import reactivemongo.core.actors.Exceptions.PrimaryUnavailableException
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.play.json._

import scala.concurrent.{ExecutionContext, Future}





/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class UserController @Inject()(val reactiveMongoApi: ReactiveMongoApi)
                              (implicit exec: ExecutionContext)
  extends Controller with MongoController with ReactiveMongoComponents {

  // defines a named logger for this class
  val logger: Logger = Logger("application." + this.getClass())

  // connect to mongoDb collection of users
  val userRepo = new UserRepo(reactiveMongoApi)




  // todo : put this code in a function taking a list of anything having an id and returning json or notfound
  // reusable code for both things and users
  /**
    * Create an async Action to return a json object response
    * containing the [[User]] selected by userId.
    *
    * @param userId id of the user to be retrieved
    * @return if userId exists, Ok status code and a User encoded as Json
    *         in the body
    *         else if userId does not exist NotFound status code
    *         else if defaut database [[database]] is not available,
    *           an InternalServerError status code
    *         else an exception I guess
    */
  def getUser(userId: String) = Action.async {
    request => {
      userRepo.findObjects(List(userId)).map {
        case user :: Nil => {
          val jsUser: JsValue = Json.toJson(user)
          logger.debug(s"tamtams : returns object from mongo ${Json.prettyPrint(jsUser)}")
          Ok(jsUser)
        }
        case Nil => {
          logger.debug(s"tamtams : thing ${userId} Not found ")
          NotFound
        }
        case _ :: _ :: xs => {
          logger.error("tamtams : found 2 or more users matching this id")
          throw new IllegalStateException
          InternalServerError("tamtams : found 2 or more users matching this id")
        }
        case _ =>{
          logger.error("tamtams : illegal state trying to getUser")
          throw new IllegalStateException
          InternalServerError("tamtams : illegal state trying to getUser (unknown returned object)")
        }
      } recover {
        // deal with exceptions related to database connection
        case PrimaryUnavailableException => {
          logger.error(s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}")
          InternalServerError
        }
      }
    }
  }


  /**
    * This action parses the body of the request
    * into a [[User]] object.
    * It inserts this [[User]] to the database,
    * creating it if it does not exist, updating it otherwise
    *
    * @param userId Id of the user to be created/updated
    *               This Id is in the uri and in the body of the request
    *               if these ids are not equal, the server answers
    *               with a forbidden answer.
    * @return
    */
  def putUser(userId: String) = Action.async(parse.json[User]) {
    request => {
      if (userId == request.body.userId) {
        logger.debug(s" tamtams : requesting insertion of User : ${request.body}")

        // (modified, upserted)
        userRepo.upsertObject(request.body).map {
          case (0, 1) => {
            logger.debug(s" tamtams : new user $userId created")
            Created.withHeaders((LOCATION, request.host + request.uri))
          }
          case (1, 0) => {
            logger.debug(s" tamtams : user updated")
            Ok.withHeaders((LOCATION, request.host + request.uri))
          }
          case _ =>{
            val msg = "putUser() illegal state trying to addUser :" + userId
            logger.error(msg)
            throw new IllegalStateException(msg)
          }
        } recover {
          // deal with exceptions related to database connection
          case err: CommandError if err.code.contains(11000) => {
            val msg = s" tamtams : MongoDb connection error ${err.getMessage()}"
            logger.error(msg)
            InternalServerError(Json.toJson(msg))
          }
          case PrimaryUnavailableException => {
            val msg = s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}"
            logger.error(msg)
            InternalServerError(Json.toJson(msg))
          }
        }
      } else {
        logger.debug(s" tamtams : userId in request is $userId is different from user.id in Json representation ${request.body.userId}")
        Future.successful(BadRequest)
      }
    }
  }


  /**
    * Create an async Action to return a json object response
    * containing the [[User]] selected by userId.
    *
    * @param userId id of the user to be deleted
    * @return if userId exists, Ok status code
    *         else if userId does not exist NotFound status code
    *         else if defaut database [[database]] is not available,
    *           an InternalServerError status code
    *         else an exception I guess
    */
  def deleteUser(userId: String) = Action.async {
    request => {
      userRepo.removeObjects(List(userId)).map {
        case 1 => {// check option contents
          logger.debug(s"tamtams : removed object from mongo ${userId}")
          Ok
        }
        case 0 => {
          logger.debug(s"tamtams : user ${userId} Not found ")
          NotFound
        }
        case _ =>{
          val msg = "tamtams : illegal state trying to deleteUser "
          logger.error(msg)
          throw new IllegalStateException(msg)
        }
      } recover {//future failed
        // deal with exceptions related to database connection
        case PrimaryUnavailableException => {
          val msg = s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}"
          logger.error(msg)
          InternalServerError(msg)
        }
      }
    }
  }

}
