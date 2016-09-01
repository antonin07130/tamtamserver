package controllers

import java.util.NoSuchElementException
import javax.inject._

import akka.util.ByteString
import play.api.Logger
import play.api.http.HttpEntity
import play.api.libs.json.{JsObject, _}
import play.api.mvc._
import utils.ThingJsonConversion._
import models.{Position, Thing, User}
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import play.modules.reactivemongo.json._
import reactivemongo.api.collections.GenericQueryBuilder
import reactivemongo.core.actors.Exceptions._
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.api.commands.{CommandError, UpdateWriteResult, WriteResult}

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


  /**
    * Connects to local database using [[database]] and
    * to TamtamUsers collection as a [[reactivemongo.api.Collection]]
    * this is a def not a val because it must be re-evaluated at each call
    */
  def usersJSONCollection : Future[JSONCollection] =
  database.map( // once future database is completed :
    connectedDb => connectedDb.collection[JSONCollection]("TamtamUsers")
  )
  // register a callback on connection error :
  usersJSONCollection.onFailure{
    case _ => logger.error(s" tamtams : MongoDb connection for {$this} error ${PrimaryUnavailableException.message}")
  }
  // register a callback on connection OK :
  usersJSONCollection.onSuccess{
    case _ => logger.debug(s" tamtams : MongoDb connection for {$this} OK")
  }





  /**
    * This function adds a Thing to a collection
    * if not found this function creates a new Thing
    *
    * @param thing [[Thing]] object to add
    * @param collection collection to update
    * @return
    */
  def addThingToThingsCollection(thing: Thing, collection: Future[JSONCollection]): Future[WriteResult] = {

    // GeoJson position : position {"type":"point","coordinates":[lon,lat]}
    val geoJsonPosition : JsObject = Json.obj(
      "position" -> Json.obj(
        "type" -> "point",
        "coordinates" -> Seq(thing.position.lon, thing.position.lon)))

    // convert our thing to Json
    val thingJson : JsObject = Json.toJson(thing).as[JsObject]

    val geoJsonThing = thingJson ++ geoJsonPosition


    // selector used in our MongoDb update (update or insert) request
    def selector = Json.obj("thingId" -> thing.thingId)
    // ask to write our User to the database
    collection.flatMap(jscol => jscol.update(selector, geoJsonThing, upsert = true))
  }

  // todo flatten Future[Option[T]] to Future[T] ? and fail the future if the option fails ?
  /**
    * Remove a [[Thing]] from a collection
    *
    * @param thingId    thingId of Thing to delete
    * @param collection collection to update
    * @return [[Future]] of [[Option]] of removed [[Thing]]
    */
  def removeThingFromThingsCollection(thingId: String,
                                      collection: Future[JSONCollection])
  : Future[Option[Thing]] ={

  logger.debug(s"tamtams : removing $thingId from database")
    collection.flatMap(jscol => jscol.findAndRemove(Json.obj("thingId" -> thingId)).map(_.result[Thing]))
  }

  // helper function to convert position node from GeoJson to Json
  def geoJsonThingToThing(geoJsonThing: JsObject): Thing = {
    val position: Position = Position(
      (geoJsonThing \ "position" \ "coordinates") (0).as[Double],
      (geoJsonThing \ "position" \ "coordinates") (1).as[Double]
    )
    val jsonPos = Json.obj("position"->position)
    val jsonThing = geoJsonThing ++ jsonPos
    jsonThing.as[Thing]
  }

  /**
    * Retrieve a thing from a mongoDb collection
    *
    * @param thingId thingId of the [[Thing]] to find
    * @param collection mongoDb collection
    * @return an [[Option]] of [[Thing]] in the [[Future]]
    */
  def getThingFromThingsCollection(thingId : String,
                                   collection: Future[JSONCollection])
  : Future[Option[Thing]] = {
  val findThingQuery: JsObject = Json.obj("thingId" -> thingId)
  val futureFindThing: Future[Option[Thing]] =
    thingsJSONCollection.flatMap(jscol => jscol.find(findThingQuery).one[JsObject].map{
      case Some(geoJsonThing) => Some(geoJsonThingToThing(geoJsonThing))
      case None => None
    })
    futureFindThing
  }

  /**
    * Retrieve a sea of Things from a mongoDb collection
    *
    * @param thingIds Seq of ThingIds to retrieve
    * @param collection Mongo Collection in which we look for Things objects
    * @return a [[Seq]] of [[Thing]]s (that can be empty)
    */
  def getListOfThingsFromThingsCollection(thingIds: Seq[String],
                                          collection: Future[JSONCollection])
  : Future[Seq[Thing]] = {
    val query = Json.obj("thingId" -> Json.obj("$in" -> (thingIds)))
    def requestForThings(query: JsObject): Future[Seq[Thing]] = {
      logger.debug(s"tamtams : executing query in mongoDb : find ${query}")
      thingsJSONCollection.flatMap(jscol => jscol.find(query).cursor[JsObject]().collect[Seq]().map(_.map(geoJsonThingToThing)))
    }
    requestForThings(query)
  }

  /**
    * Add a newValue to an array of [[String]]s within a user object
    * in a collection containing users
    *
    * @param userId : id of the user whose array of things will be updated
    * @param userArrayName : name as [[String]] of the array to update
    * @param newValue : value as [[String]] to insert in the array
    * @param collection : collection to update
    * @return [[Future]] of the [[WriteResult]] of this operation
    */
  def addValueToUserArray(userId : String, userArrayName : String, newValue: String, collection: Future[JSONCollection])
  : Future[WriteResult] = {
    def selector = Json.obj("userId" -> userId)

    def insertionRequest = Json.obj( "$addToSet" -> Json.obj(userArrayName -> newValue))

    logger.debug(s"tamtams : update request to mongoDb : $selector $insertionRequest")

    collection.flatMap(jscol => jscol.update(selector, insertionRequest, upsert = true))
  }

  /**
    * remove a valueToRemove from an array of [[String]]s within a user object
    * in a collection containing users
    *
    * @param userId : id of the user whose array of things will be updated
    * @param userArrayName : name as [[String]] of the array to update
    * @param valueToRemove : value as [[String]] to remove from the array
    * @param collection : collection to update
    * @return [[Future]] of the [[WriteResult]] of this operation
    */
  def removeValueFromUserArray(userId: String, userArrayName: String, valueToRemove: String, collection: Future[JSONCollection])
  : Future[WriteResult] = {
    def selector = Json.obj("userId" -> userId)
    // define a mongoDb request to remove the value from the userArray
    def removalRequest = Json.obj("$pull" -> Json.obj(userArrayName -> valueToRemove))

    // query the database
    collection.flatMap(jscol => {
      logger.debug(s"tamtams : executing query in mongoDb : update ${selector}, request: ${removalRequest}")
      jscol.update(selector, removalRequest)
    })
  }



  def getArrayFromUser(userId: String, userArrayName: String, collection: Future[JSONCollection])
  : Future[Option[Seq[String]]] = {
    val findUserQuery: JsObject = Json.obj("userId" -> userId)
    val projectOnlyArray: JsObject = Json.obj(userArrayName -> 1, "_id" -> 0)

    // try to get a Js value containing an array named "userArrayName"
    val futureSellingThingsJson: Future[Option[JsValue]] = {
      logger.debug(s"tamtams : executing query in mongoDb : find ${findUserQuery}, projection : ${projectOnlyArray}")
      collection.flatMap(jscol => jscol.find(findUserQuery, projectOnlyArray).one[JsValue])
    }
    // try to extract the array and store it as a Seq[String]
    futureSellingThingsJson.flatMap {
      case Some(jsonVal) => {
        val ar = (jsonVal \ userArrayName).as[Seq[String]]
        logger.debug(s"tamtams : returning following array from MongoDb : ${ar}")
        Future.successful(Some(ar))
      }
      case None => {
        logger.debug(s"tamtams : no array named ${userArrayName} found in mongoDb for ${userId}")
        Future.failed(throw new NoSuchElementException(s"user ${userId} NotFound in mongoDb"))
      }
    }
  }



  // todo : put this code in a function taking a list of anything having an id and returning json or notfound
  /**
    * Create an async Action to return a json object response
    * containing the [[Thing]] selected byuserId.
    *
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

      val futureFindThing = getThingFromThingsCollection(thingId, usersJSONCollection)
      futureFindThing.map {
        case Some(thing) => {
          val jsonThing: JsValue = Json.toJson(thing)
          logger.debug(s"tamtams : returns object from mongo ${Json.prettyPrint(jsonThing)}")
          Ok(jsonThing)
        }
        case None => {
          logger.debug(s"tamtams : thing ${thingId} Not found ")
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
    * This async action reads a request body
    * and parses it into a [[Thing]] object.
    * The [[Thing]] object is inserted in the database.
    *
    * @deprecated be cautious, this does not update user collection
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
      val futureWriteThingResult = addThingToThingsCollection(request.body,thingsJSONCollection)

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



// todo : merge the 2 db requests results to 1 global result to handle errors...
  /**
    * This async action reads a request body
    * and parses it into a [[Thing]] object.
    * The [[Thing]] object thingId is inserted in the
    * userId's sellingThings list.
    *
    * @param userId  Id of the user whose sellingThings list will be updated.
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


      // todo : write a for comprehension here to chain future actions
      // ask to write our Thing to the database
      val futureWriteThingResult = addThingToThingsCollection(request.body,thingsJSONCollection)

      // ask to write our Thing to the database
      val futureWriteThingIdResult: Future[WriteResult] =
        addValueToUserArray(userId, "sellingThings", request.body.thingId, usersJSONCollection)
      // stick callbacks to write results to send an appropriate answer
      futureWriteThingIdResult.map { okResult =>
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
    *
    * @param userId  Id of the user whose sellingThings list will be updated.
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


      // todo : write a for comprehension here to chain future actions
      // ask to remove our Thing to the database
      val futureRemovedThing : Future[Option[Thing]] =
        removeThingFromThingsCollection(thingId,thingsJSONCollection)

      val futureRemoveThingIdResult: Future[WriteResult] =
        removeValueFromUserArray(userId, "sellingThings" ,thingId, usersJSONCollection)

      // combine both futures to check for errors :
      val futureQueriesResults: Future[(Option[Thing], WriteResult)] =
        futureRemovedThing.zip(futureRemoveThingIdResult)
      /*
      // helper function to check query results are as expected
      def verifyQueryResults(queryResults : (Option[Thing], WriteResult)) : Status = {
        queryResults match {
          Some(thing),UpdateWriteResult(_,_,1,_,_,_,_,_)

        val checkWriteResult = if (queryResults._2.nModified > 0) true
        else false
        checkWriteResult && checkOption
      }
      */

      // stick callbacks to write results to send an appropriate answer
      futureQueriesResults.map { okResult => // of for the database but...
        if (verifyQueryResults(okResult)) {
        logger.debug(s" tamtams : sucessfull removal of from MongoDb from ${userId} : ${okResult}")
        Ok
        }
        else
          InternalServerError
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
    * This action retrieves the list of things a user is selling
    * and sends it a a Json array of [[Thing]]
    *
    * @param userId
    * @return
    */
  def getSellingThings(userId: String) = Action.async {
    request => {


      // get the list of things (first get array of Ids, then request for ids in things collection)
      val listOfThings: Future[Seq[Thing]] = getArrayFromUser(userId, "sellingThings", usersJSONCollection).flatMap {
        case Some(thingIds) => getListOfThingsFromThingsCollection(thingIds, usersJSONCollection)
        case None => throw new NoSuchElementException("No result from user request")
      }

      listOfThings.map {
        case listOfThings: Seq[Thing] => {
          // sucessful future with good value
          val jsonThingsList: JsValue = Json.toJson(listOfThings)
          logger.debug(s"tamtams : returns things from mongo ${Json.prettyPrint(jsonThingsList)}")
          Ok(jsonThingsList)
        }
        case _ => {
          logger.error(s"tamtams : collection structure unknown ($thingsJSONCollection should be a collection of Things) ")
          InternalServerError
        }
      } recover {
        case e: NoSuchElementException => {
          // if we were not able to build the request
          logger.debug(s"tamtams : could not build list of Thing Ids for user ${userId}")
          NotFound
        }
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



/*

      val futureThingIdArray: Future[Seq[String]] = futureFindUser.map(_.get.sellingThings)

      val jsonlistofThings: Future[JsValue] = futureThingIdArray.map(idList => Json.toJson(idList))

      val futureListThings: Future[List[Thing]] =
        usersJSONCollection.flatMap(jscol => jscol.find(


      // tranform User sellingThings array to JSON
      //
      val futureThingsArray : Future[Seq[String]] = futureFindUser.map(_.get.sellingThings)
  */
      /*
      // Fetch all the Parts that are linked to this Product
      > sellingThings = db.Things.find({thingId: { $in : user.sellingThingss } } ).toArray() ;
      */

  }


  // todo : get things near with database near functions
  def getThingsNear(lat: Double, lon: Double) = TODO
  def getThings = TODO

}
