package controllers

import javax.inject._

import akka.util.ByteString
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.mvc._
import utils.ThingJsonConversion._
import models.Thing
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
    case _ => Logger.error(s" tamtams : MongoDb connection for {$this} error ${PrimaryUnavailableException.message}")
  }
  // register a callback on connection OK :
  thingsJSONCollection.onSuccess{
    case _ => Logger.info(s" tamtams : MongoDb connection for {$this} OK")
  }


  // todo : put this code in a function taking a list of anything having an id and returning json or notfound
  /**
    * Create an async Action to return a json object response
    * containing the [[Thing]] selected by userId.
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

      val findThingQuery: JsObject = Json.obj("id" -> thingId)
      val futureFindThing: Future[Option[Thing]] =
        thingsJSONCollection.flatMap(jscol => jscol.find(findThingQuery).one[Thing])

      futureFindThing.map{
        case Some(thing) => {
          val jsonThing: JsValue = Json.toJson(thing)
          Logger.info(s"tamtams : returns object from mongo ${Json.prettyPrint(jsonThing)}")
          Ok(Json.toJson(jsonThing))// todo : check if necessary to reencode as json ?
        }
        case None => {
          Logger.info(s"tamtams : thing ${thingId} Not found ")
          NotFound
        }
      } recover { // deal with exceptions related to database connection
        case PrimaryUnavailableException => {
          Logger.error(s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}")
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
      if (thingId == request.body.id) {
      Logger.info(s" tamtams : requesting insertion of Thing : ${request.body}")

      // ask to write our Thing to the database
      val futureWriteThingResult: Future[WriteResult] =
        thingsJSONCollection.flatMap(jscol => jscol.insert[Thing](request.body))

      // stick callbacks to write results to send an appropriate answer
      futureWriteThingResult.map{ okResult =>
        Logger.info(s" tamtams : sucessfull insertion to MongoDb ${okResult}")
        Created.withHeaders((LOCATION, request.host + request.uri))
      } recover { // deal with exceptions related to database connection
        case err: CommandError if err.code.contains(11000) => {
          Logger.error(s" tamtams : MongoDb connection error ${err.getMessage()}")
          InternalServerError
        }
        case PrimaryUnavailableException => {
          Logger.error(s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}")
          InternalServerError
        }
      }
      }
      else
        {
          Logger.info(s" tamtams : thingId in request is $thingId is different from thing.id in Json representation ${request.body.id}")
          Future.successful(BadRequest)
        }
    }
  }


  // todo : get things near with database near functions
  def getThingsNear(lat: Double, lon: Double) = TODO
  def getThings = TODO

}
