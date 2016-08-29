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
import reactivemongo.api.commands.{CommandError, WriteResult}

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
    * This function adds a Thing to a collection
    * if not found this function creates a new Thing
    *
    * @param thing
    * @param collection collection to update
    * @return
    */
  def addThingToThingsCollection(thing: Thing, collection: Future[JSONCollection]): Future[WriteResult] = {
    // selector used in our MongoDb update (update or insert) request
    def selector = Json.obj("thingId" -> thing.thingId)
    // ask to write our User to the database
    collection.flatMap(jscol => jscol.update[JsObject, Thing](selector, thing, upsert = true))
  }

  // todo flatten Future[Option[T]] to Future[T] ? and fail the future if the option fails ?
  /**
    * Remove a [[Thing]] from a collection
    *
    * @param thingId
    * @param collection collection to update
    * @return [[Future]] of [[Option]] of removed [[Thing]]
    */
  def removeThingFromThingsCollection(thingId: String, collection: Future[JSONCollection]): Future[Option[Thing]] =
  collection.flatMap(jscol => jscol.findAndRemove(Json.obj("thingId" -> thingId)).map(_.result[Thing]))


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
    // now write thingId to user's selling thing list
    def selector = Json.obj("userId" -> userId)

    //add to array of sellingthings the new thing :
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
    // define a mongoDb request to remove the thingId fom sellingThings
    def removalRequest = Json.obj("$pull" -> Json.obj(userArrayName -> valueToRemove))
    logger.debug(s"tamtams : update request to mongoDb : $selector $removalRequest")

    // ask to write our Thing to the database
    thingsJSONCollection.flatMap(jscol => {
      jscol.update(selector, removalRequest)
    })
  }




  /**
    * This async action reads a request body
    * and parses it into a [[Thing]] object.
    * The [[Thing]] object is inserted in the database.
    *
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

      //todo write thhingref to User's selling things

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


      // stick callbacks to write results to send an appropriate answer
      futureRemoveThingIdResult.map { okResult =>
        logger.debug(s" tamtams : sucessfull removal of from MongoDb from ${userId} : ${okResult.errmsg}")
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


/*
  def getSellingThings(userId : String) = Action.async {
    request => {

      // fetch the user identified by userId
      val findUserQuery: JsObject = Json.obj("idUser" -> userId)
      val futureFindUser: Future[Option[User]] =
        usersJSONCollection.flatMap(jscol => jscol.find(findUserQuery).one[User])


      val futureThingIdArray: Future[Seq[String]] = futureFindUser.map(_.get.sellingThings)

      val jsonlistofThings: Future[JsValue] = futureThingIdArray.map(idList => Json.toJson(idList))

      val futureListThings: Future[List[Thing]] =
        usersJSONCollection.flatMap(jscol => jscol.find(


      // tranform User sellingThings array to JSON
      //
      val futureThingsArray : Future[Seq[String]] = futureFindUser.map(_.get.sellingThings)
      
      /*
      // Fetch all the Parts that are linked to this Product
      > sellingThings = db.Things.find({thingId: { $in : user.sellingThingss } } ).toArray() ;
      */
      Future(Ok)
    }
  }
*/

  // todo : get things near with database near functions
  def getThingsNear(lat: Double, lon: Double) = TODO
  def getThings = TODO

}
