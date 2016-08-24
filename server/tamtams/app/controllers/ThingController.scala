package controllers

import javax.inject._

import akka.util.ByteString
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.mvc._
import utils.ThingJsonConversion._
import models.Thing
import models.User

import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.core.actors.Exceptions._
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.api.commands.{ CommandError, WriteResult }

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * This controller creates asynchronous Actions to handle HTTP requests
  * related to [[Thing]].
  * It connects to a MongoDb database to serve requests
  * using [[ReactiveMongoApi]].
  */
@Singleton
class ThingController @Inject() (val reactiveMongoApi: ReactiveMongoApi)
                                (implicit exec: ExecutionContext)
  extends Controller with MongoController with ReactiveMongoComponents {

  // defines a named logger for this class
  val logger: Logger = Logger("application." + this.getClass())

  /**
    * Connects to local database using [[database]] and
    * to TamtamThings collection as a [[reactivemongo.api.Collection]]
    * this is a def not a val because it must be re-evaluated at each call
    */
  def thingsJSONCollection : Future[JSONCollection] =
    database.map( // once future database is completed :
      connectedDb => connectedDb.collection[JSONCollection]("TamtamThings")
    )
  // register a callback on connection error :
  thingsJSONCollection.onFailure{
    case _ => logger.error(s" tamtams : MongoDb connection for {$this} error ${PrimaryUnavailableException.message}")
  }
  // register a callback on connection OK :
  thingsJSONCollection.onSuccess{
    case _ => logger.debug(s" tamtams : MongoDb connection for {$this} OK")
  }


  // todo : put this code in a function taking a list of anything having an id and returning json or notfound
  /**
    * Create an async Action to return a json object response
    * containing the [[Thing]] selected byuserId.
    * @param thingId id of the thing to be retrieved
    * @return if thingId exists, Ok status code and a Thing encoded as Json
    *           in the body
    *         else if thingId does not exist NotFound status code
    *         else if defaut database [[database]] is not available,
    *           an InternalServerError status code
    *         else an exception I guess
    */
  def getThing(thingId: String) = Action.async {
    request => {

      val findThingQuery: JsObject = Json.obj("thingId" -> thingId)

      val futureFindThing: Future[Option[Thing]] =
        thingsJSONCollection.flatMap(jscol => jscol.find(findThingQuery).one[Thing])

      futureFindThing.map{
        case Some(thing) => {
          val jsonThing: JsValue = Json.toJson(thing)
          logger.debug(s"tamtams : returns object from mongo ${Json.prettyPrint(jsonThing)}")
          Ok(jsonThing)
        }
        case None => {
          logger.debug(s"tamtams : thing ${thingId} Not found ")
          NotFound
        }
      } recover { // deal with exceptions related to database connection
        case PrimaryUnavailableException => {
          logger.error(s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}")
          InternalServerError
        }
      }
    }
  }


  /**
    * This async action reads a request body
    * and parses it into a [[Thing]] object.
    * The [[Thing]] object is inserted in the database.
    * @param thingId Id of the thing to be created
    *                This Id is also part of the Json (Thing/id)
    *                if these ids are not equal, the server answers
    *                with a BadRequest answer.
    * @return
    */
  def putThing(thingId: String) = Action.async(parse.json[Thing]) {
    request => {
      if (thingId == request.body.thingId) {
      logger.debug(s" tamtams : requesting insertion of Thing : ${request.body}")

      // ask to write our Thing to the database
      val futureWriteThingResult: Future[WriteResult] =
        thingsJSONCollection.flatMap(jscol => jscol.insert[Thing](request.body))

      // stick callbacks to write results to send an appropriate answer
      futureWriteThingResult.map{ okResult =>
        logger.debug(s" tamtams : sucessfull insertion to MongoDb ${okResult}")
        Created.withHeaders((LOCATION, request.host + request.uri))
      } recover { // deal with exceptions related to database connection
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
      else
        {
          logger.debug(s" tamtams : thingId in request is $thingId is different from thing.id in Json representation ${request.body.thingId}")
          Future.successful(BadRequest)
        }
    }
  }



// todo : all this function
  /**
    * This async action reads a request body
    * and parses it into a [[Thing]] object.
    * The [[Thing]] object is inserted in the
    *userId's sellingThings list.
    *
    * @paramuserId  Id of the user whose sellingThings list will be updated.
    *                If this user is not found, a not found response is sent.
    * @param thingId Id of the thing to be created
    *                This Id is also part of the Json (Thing/id)
    *                if these ids are not equal, the server answers
    *                with a BadRequest answer.
    * @return
    */
  def sellThing(userId: String, thingId: String) = Action.async(parse.json[Thing]) {
    request => {
      if (thingId != request.body.thingId) {
        logger.debug(s" tamtams : thingId in request is $thingId is different from thing.id in Json representation ${request.body.thingId}")
        Future.successful(BadRequest)
      }


      def selector = Json.obj("userId" ->userId)
      // define a mongoDb request to push the Json representation of thing to an array named sellingThings
      def insertionRequest = Json.obj(
        "$addToSet" -> Json.obj(
          "sellingThings" -> Json.toJson(request.body))
      )

      // ask to write our Thing to the database
      val futureWriteThingResult: Future[WriteResult] =
      thingsJSONCollection.flatMap(jscol => {
        logger.debug(s"request to mongoDb : $selector $insertionRequest")
        jscol.update(selector, insertionRequest, upsert = true)
      })

      // stick callbacks to write results to send an appropriate answer
      futureWriteThingResult.map { okResult =>
        logger.debug(s" tamtams : sucessfull insertion to MongoDb in ${userId} : ${okResult.errmsg}")
        Created.withHeaders((LOCATION, request.host + request.uri))
      } recover {
        // deal with exceptions related to database connection
        case err: CommandError => {
          logger.error(s" tamtams : MongoDb command error ${err.getMessage()}")
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
    * This async action finds and remove the [[Thing]] object
    * fromuserId's sellingThings list.
    * @paramuserId  Id of the user whose sellingThings list will be updated.
    *                If this user is not found, a not found response is sent.
    * @param thingId Id of the thing to be created
    *                This Id is also part of the Json (Thing/id)
    *                if these ids are not equal, the server answers
    *                with a BadRequest answer.
    * @return
    */
  def removeThing(userId: String, thingId: String) = Action.async {
    request => {
      logger.debug(s" tamtams : requesting removal of Thing with Id: ${thingId} from user with Id: ${userId}")

      def selector = Json.obj("userId" ->userId)
      // define a mongoDb request to push the Json representation of thing to an array named sellingThings
      def removalRequest = Json.obj("$pull" -> Json.obj("sellingThings" -> Json.obj("thindId" -> thingId)))

      // ask to write our Thing to the database
      val futureWriteThingResult: Future[WriteResult] =
      thingsJSONCollection.flatMap(jscol => {
        logger.debug(s"request to mongoDb : $selector $removalRequest")
        jscol.update(selector, removalRequest)
      })

      // stick callbacks to write results to send an appropriate answer
      futureWriteThingResult.map { okResult =>
        logger.debug(s" tamtams : sucessfull removal from MongoDb from ${userId} : ${okResult.errmsg}")
        Ok
      } recover {
        // deal with exceptions related to database connection
        case err: CommandError => {
          logger.error(s" tamtams : MongoDb command error ${err.getMessage()}")
          InternalServerError
        }
        case PrimaryUnavailableException => {
          logger.error(s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}")
          InternalServerError
        }
      }
    }
  }





  // todo : get things near with database near functions
  def getThingsNear(lat: Double, lon: Double) = TODO
  def getThings = TODO

}
