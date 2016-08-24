package controllers

import javax.inject._

import play.api.Logger
import play.api.mvc._
import models.User
import utils.UserJsonConversion._
import play.api.libs.json._
import play.libs.Json._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.commands.{CommandError, WriteResult}
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

  /**
    * Connects to local database using [[database]] and
    * to TamtamUsers collection as a [[reactivemongo.api.Collection]]
    * this is a def not a val because it must be re-evaluated at each call
    */

  def usersJSONCollection: Future[JSONCollection] =
  database.map(// once future database is completed :
    connectedDb => connectedDb.collection[JSONCollection]("TamtamThings")
  )

  // register a callback on connection error :
  usersJSONCollection.onFailure {
    case _ => logger.error(s" tamtams : MongoDb connection for {$this} error ${PrimaryUnavailableException.message}")
  }
  // register a callback on connection OK :
  usersJSONCollection.onSuccess {
    case _ => logger.debug(s" tamtams : MongoDb connection for {$this} OK")
  }


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

      val findUserQuery: JsObject = Json.obj("idUser" -> userId)
      val futureFindUser: Future[Option[User]] =
        usersJSONCollection.flatMap(jscol => jscol.find(findUserQuery).one[User])

      futureFindUser.map {
        case Some(user) => {
          val jsonUser: JsValue = Json.toJson(user)
          logger.debug(s"tamtams : returns object from mongo ${Json.prettyPrint(jsonUser)}")
          Ok(jsonUser)
        }
        case None => {
          logger.debug(s"tamtams : user ${userId} Not found ")
          NotFound
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
      if (userId != request.body.userId) {
        logger.debug(s" tamtams : userId in request is $userId is different from user.id in Json representation ${request.body.userId}")
        Future.successful(BadRequest)
      }

      logger.debug(s" tamtams : requesting insertion of User : ${request.body}")


      // selector used in our MongoDb update (update or insert) request
      def selector = Json.obj("idUser" -> userId)

      // ask to write our User to the database
      val futureWriteUserResult: Future[WriteResult] =
      usersJSONCollection.flatMap(jscol => jscol.update(selector, request.body, upsert = true))

      // stick callbacks to write results to send an appropriate answer
      futureWriteUserResult.map { okResult =>
        logger.debug(s" tamtams : sucessfull insertion to MongoDb ${okResult}")
        Created.withHeaders((LOCATION, request.host + request.uri))
      } recover {
        // deal with exceptions related to database connection
        case err: CommandError if err.code.contains(11000) => {
          logger.error(s" tamtams : MongoDb connection error ${err.getMessage()}")
          InternalServerError
        }
        case PrimaryUnavailableException => {
          logger.error(s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}")
          InternalServerError
        }
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

      val findUserQuery: JsObject = Json.obj("idUser" -> userId)

      val futureRemovedUser: Future[Option[User]] =
        usersJSONCollection.flatMap(jscol => jscol.findAndRemove(findUserQuery).map(_.result[User]))

      futureRemovedUser.map {//future was successfull
        case Some(user) => {// check option contents
          val jsonUser: JsValue = Json.toJson(user)
          logger.debug(s"tamtams : removed object from mongo ${Json.prettyPrint(jsonUser)}")
          Ok
        }
        case None => {
          logger.debug(s"tamtams : user ${userId} Not found ")
          NotFound
        }
      } recover {//future failed
        // deal with exceptions related to database connection
        case PrimaryUnavailableException => {
          logger.error(s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}")
          InternalServerError
        }
      }
    }
  }



}
