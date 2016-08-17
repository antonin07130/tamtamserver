package controllers

import javax.inject._

import akka.util.ByteString
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.mvc._
import logic.JsonOConversion._
import logic.JsonConversion._
import logic.Thing
import logic.ThingsGenerator
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.core.actors.Exceptions._
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.api.commands.{ CommandError, WriteResult }

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * This controller creates Actions to handle HTTP requests
  * related to [[Thing]]
  * it connects to a MongoDb database to serve requests
  */
@Singleton
class ThingController @Inject() (val reactiveMongoApi: ReactiveMongoApi)
                                (implicit exec: ExecutionContext)
  extends Controller with MongoController with ReactiveMongoComponents {


  // connect to local database using [[database]] and
  // to TamtamThings collection as a [[reactivemongo.api.Collection]]
  // this is a def not a val because it must be re-evaluated at each call
  //
  def thingsJSONCollection : Future[JSONCollection] =
    database.map( // once future database is completed :
      connectedDb => connectedDb.collection[JSONCollection]("TamtamThings")
    )

  // register a callback on connection error :
  thingsJSONCollection.onFailure{
    case _ => Logger.error(s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}")
  }

  // register a callback on connection OK :
  thingsJSONCollection.onSuccess{
    case _ => Logger.info(s" tamtams : MongoDb connection OK")
  }

  // todo : put this code in a function taking a list of anything having an id and returning json or notfound
  def getThing(thingId: String) = Action.async {
    request => {

      val findThingQuery: JsObject = Json.obj("id" -> thingId)
      val futureFindThing: Future[Option[Thing]] =
        thingsJSONCollection.flatMap(jscol => jscol.find(findThingQuery).one[Thing])

      futureFindThing.map{
        case Some(thing) => {
          val jsonThing: JsValue = Json.toJson(thing)
          Logger.info(s"tamtams : returns object from mongo ${Json.prettyPrint(jsonThing)}")
          Ok(Json.toJson(jsonThing))
        }
        case None => {
          Logger.info(s"tamtams : thing ${thingId} Not found ")
          NotFound
        }
      } recover { // deal with exceptions related to database connection
        case PrimaryUnavailableException => {Logger.error(s" tamtams : MongoDb connection error ${PrimaryUnavailableException.message}")
          InternalServerError
        }
      }
    }
  }


  // TODO : test and return URI with created status
  /**
    * As of today this action can take a Json as a parameter
    * and parse it into a [[Thing]] object.
    * It answers with a bad request when unhappy
    *
    * @param thingId Id of the thing to be created
    *                This Id is also part of the Json (Thing/id)
    * @return
    */
  def putThing(thingId: String) = Action.async(parse.json[Thing]) {
    request => {
      Logger.info(s" tamtams : requesting insertion of Thing : ${request.body}")

      // ask to write our Thing to the database
      val futureWriteThingResult: Future[WriteResult] =
        thingsJSONCollection.flatMap(jscol => jscol.insert[Thing](request.body))

      // stick callbacks to write results to send an appropriate answer
      futureWriteThingResult.map{ okResult =>
        Logger.info(s" tamtams : sucessfull insertion to MongoDb ${okResult}")
        Created(request.host+request.uri)
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
  }


  // todo : get things near with database near functions
  def getThingsNear(lat: Double, lon: Double) = TODO
  def getThings = TODO

}
