package controllers

import java.util.NoSuchElementException
import javax.inject._

import scala.concurrent.{ExecutionContext, Future}

import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.api.commands._
import reactivemongo.core.actors.Exceptions._
import reactivemongo.play.json._

import database.{ThingRepo, UserRepo}
import models.Thing
import utils.ThingJsonConversion._
/**
  * This controller creates asynchronous Actions to handle HTTP requests
  * related to [[Thing]].
  * It connects to a MongoDb database to serve requests
  * using [[ReactiveMongoApi]].
  */
@Singleton
class ThingController @Inject()(val reactiveMongoApi: ReactiveMongoApi)
                               (implicit exec: ExecutionContext)
  extends Controller with MongoController with ReactiveMongoComponents {

  // defines a named logger for this class
  val logger: Logger = Logger("application." + this.getClass())

  val thingRepo = new ThingRepo(reactiveMongoApi)
  val userRepo = new UserRepo(reactiveMongoApi)


  // helper partial function to deal with database based requests
  def DbExceptionResults: PartialFunction[Throwable, Result] = {
    case PrimaryUnavailableException => {
      logger.error(s"tamtams : MongoDb connection error ${PrimaryUnavailableException.message}")
      InternalServerError
    }
    case err: CommandError => {
      logger.error(s"tamtams : MongoDb command error ${err.getMessage()}")
      InternalServerError
    }
  }


  /**
    * Create an async Action to return a json object response
    * containing the [[Thing]] selected byuserId.
    *
    * @param thingId id of the thing to be retrieved
    * @return if thingId exists, Ok status code and a Thing encoded as Json
    *         in the body
    *         else if thingId does not exist NotFound status code
    *         else if defaut database [[database]] is not available,
    *         an InternalServerError status code
    *         else an exception I guess
    */
  def getThing(thingId: String) = Action.async {
    request => {

      val futureFindThing = thingRepo.findObjects(List(thingId))
      futureFindThing.map {
        case thing :: Nil => {
          val jsonThing: JsValue = Json.toJson(thing)
          logger.debug(s"tamtams : returns object from mongo ${Json.prettyPrint(jsonThing)}")
          Ok(jsonThing)
        }
        case Nil => {
          logger.debug(s"tamtams : thing ${thingId} Not found ")
          NotFound
        }
        case _ :: _ :: xs => {
          logger.error(s"tamtams : found 2 or more elements matching this id")
          InternalServerError
        }
      } recover {
        DbExceptionResults
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
        val futureWriteThingResult = thingRepo.upsertObject(request.body)

        // stick callbacks to write results to send an appropriate answer
        futureWriteThingResult.map { okResult =>
          logger.debug(s" tamtams : sucessfull insertion to MongoDb ${okResult}")
          Created.withHeaders((LOCATION, request.host + routes.ThingController.getThing(thingId)))
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
      else {
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
      else {
        // ask to write our thingId to user collection in sellingThings array
        val futureWriteThingIdResult: Future[WriteResult] =
        userRepo.addValueToUserArray(userId, "sellingThings", request.body.thingId)

        // when user is updated, ask to update thing database (hope that execution order is conserved !)
        val futureWriteThingResult: Future[WriteResult] = futureWriteThingIdResult.flatMap {
          case UpdateWriteResult(_, 0, 0, _, _, _, _, _) => // user was not found : do nothing and send fake write result
            Future.successful(UpdateWriteResult(true, 0, 0, Seq(), Seq(), None, None, None))
          case UpdateWriteResult(true, 1, _, _, _, _, _, None) => // user found
            thingRepo.upsertObject(request.body) //update Things collection
          case _ => Future.failed(throw new IllegalStateException) // should not happen
        }



        // combine both futures to check for errors :
        val futureInsertQueriesResults: Future[(WriteResult, WriteResult)] =
        futureWriteThingIdResult.zip(futureWriteThingResult)

        //noinspection RedundantBlock
        // helper function to check that query results are as expected
        // todo : correct writeresult checks in this function
        def verifyInsertsQueriesResults(queryResults: (WriteResult, WriteResult)): Result = {
          logger.debug(s"$queryResults")
          queryResults match {
            case (UpdateWriteResult(true,1,1,_,_,None,None,None),UpdateWriteResult(true,1,0,_,List(),None,None,None)) => {
              logger.debug(s"tamtams - insertion in user collection ok,\n new thing $thingId in thing collection ok")
              Created.withHeaders((LOCATION, request.host + routes.ThingController.getThing(thingId)))
            }
            case (UpdateWriteResult(true,1,0,List(),List(),None,None,None),UpdateWriteResult(true,1,1,List(),List(),None,None,None)) => {
              logger.debug(s"tamtams : $thingId already in user $userId collection, update in thing collection OK")
              Ok
            }
            case (UpdateWriteResult(true, 1, 1, _, _, _, _, _), UpdateWriteResult(false, _, _, _, _, _, _, _)) => {
              logger.error(s"tamtams : insertion of thing $thingId in user $userId collection ok, in thing collection KO") //todo : correct state
              InternalServerError
            }
            case (UpdateWriteResult(_, 0, 0, _, _, _, _, _), UpdateWriteResult(true, 1, _, _, _, _, _, _)) => {
              logger.error(s"tamtams : user $userId not found, but insertion of thing $thingId in thing collection happened anyways") // todo : correct inconsistent state
              InternalServerError
            }
            case (UpdateWriteResult(_, 0, _, _, _, _, _, _), UpdateWriteResult(_, 0, _, _, _, _, _, _)) => {
              logger.debug("tamtams : not found : did not update user nor thing collection")
              NotFound
            }
            case (_, _) => {
              logger.error("tamtams : unspecified state")
              throw new IllegalStateException()
            }
          }
        }


        // stick callbacks to write results to send an appropriate answer
        futureInsertQueriesResults.map { okResult =>
          verifyInsertsQueriesResults(okResult)
        } recover { // deal with exceptions related to database connection
          DbExceptionResults
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


      // todo : maybe chain actions (to look for user first and then delete object ?)
      // ask to remove our Thing from things collection
      val futureRemovedThing: Future[WriteResult] =
      thingRepo.removeObjects(List(thingId))
      // ask to remove the thingId from SellingThings array in users collection
      val futureRemoveThingIdResult: Future[WriteResult] =
      userRepo.removeValueFromUserArray(userId, "sellingThings", thingId)

      // combine both futures to check for errors :
      val futureQueriesResults: Future[(WriteResult, WriteResult)] =
      futureRemovedThing.zip(futureRemoveThingIdResult)

      // todo : correct writeresult checks in this function
      // helper function to check that query results are as expected
      def verifyRemoveQueriesResults(queryResults: (WriteResult, WriteResult)): Result = {
        logger.debug(s"$queryResults")
        queryResults match {
          case (DefaultWriteResult(true,1,List(),None,None,None), UpdateWriteResult(true,1,1,List(),List(),None,None,None))  => {
            logger.debug(s"tamtams - deletion of thing $thingId in user ${userId} and in thing db are sucessfull")
            Ok
          }
          case (DefaultWriteResult(true,1,List(),None,None,None), UpdateWriteResult(true,1,0,List(),List(),None,None,None)) => {
            logger.error(s"tamtams : thing $thingId deleted from thing collection but not found in user $userId")
            InternalServerError
          }
          case (DefaultWriteResult(true,0,List(),None,None,None),UpdateWriteResult(true,1,1,List(),List(),None,None,None)) => {
            logger.error(s"tamtams : thing $thingId deleted from user $userId collection but not found in thing collection")
            InternalServerError
          }
          case (DefaultWriteResult(true,0,List(),None,None,None),UpdateWriteResult(true,1,0,List(),List(),None,None,None)) => {
            logger.debug("tamtams : not found : did not update user nor thing collection")
            NotFound
          }
          case (_, _) => {
            logger.error("tamtams : unspecified state")
            throw new IllegalStateException()
          }
        }
      }

      // stick callbacks to write results to send an appropriate answer
      futureQueriesResults.map {
        // results free of exceptions but still need to check databases operations results
        okResult => verifyRemoveQueriesResults(okResult)
      } recover { // deal with exceptions related to database connection
        DbExceptionResults
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
      val listOfThings: Future[Seq[Thing]] = userRepo.getArrayFromUser(userId, "sellingThings").flatMap {
        case Some(thingIds) => thingRepo.findObjects(thingIds)
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
          logger.error(s"tamtams : collection structure unknown ($thingRepo.thingsJSONCollection should be a collection of Things) ")
          InternalServerError
        }
      } recover { // deal with exceptions related to database connection
        DbExceptionResults andThen { // and then other exceptions
          case e: NoSuchElementException => {
            // if we were not able to build the request
            logger.debug(s"tamtams : could not build list of Thing Ids for user ${userId}")
            NotFound
          }
        }
      }
    }
  }


  /**
    * This redirection is here to provide a coherent collection access API
    * absolutely no check on values.
    *
    * @param userId  not checked userId
    * @param thingId id of the Thing to retrieve
    * @return redirect to ressource location
    */
  def getSellingThing(userId: String, thingId: String) = Action {
    request => {
      Redirect(request.host + routes.ThingController.getThing(thingId))
    }
  }

  /**
    * This action retrieves things around a geographical position
    * The return type is an array of distance and thing :
    * [
    * {dis:x,obj:Thing1},
    * {dis:y,obj:Thing2},
    * ...
    * ]
    *
    * @param lon         longitude
    * @param lat         latitude
    * @param maxDistance maximum distance in meter
    * @param num         maximum number results
    * @return
    */
  def getThingsNear(lon: Double, lat: Double, maxDistance: Option[Double], num: Option[Int]) = Action.async {
    request => {

      val futureJsDisAndThing = thingRepo.findNear(lon, lat, maxDistance, num)

      futureJsDisAndThing.map {
        case jsGeoResult: Seq[JsObject] => {
          // sucessful future with a supposedly good array...
          val jsre = Json.toJson(jsGeoResult)
          logger.debug(s"tamtams : returns geoNear from mongo ${Json.prettyPrint(jsre)}")
          Ok(jsre)
        }
        case _ => {
          logger.error(s"tamtams : geoNear did not return expected array of dis and obj ")
          InternalServerError
        }
      } recover {
        DbExceptionResults andThen {
          case err: NoSuchElementException => {
            logger.error(s"tamtams Things Collection not found : ${err.getMessage()}")
            InternalServerError
          }
        }
      }
    }
  }

  /**
    * This action returns the complete Actions database as an array of Things
    *
    * @deprecated be careful : no limit on number of results : can stale db or server
    * @return array of [[Thing]] s in Json
    */
  def getThings = Action.async {
    request => {

      logger.debug(s"tamtams : listing complete Things collection")
      val allThings = thingRepo.findObjects()

      allThings.map {
        case listOfThings: Seq[Thing] => {
          // sucessful future with good value
          val jsonThingsList: JsValue = Json.toJson(listOfThings)
          logger.debug(s"tamtams : returns things from mongo ${Json.prettyPrint(jsonThingsList)}")
          Ok(jsonThingsList)
        }
        case _ => {
          logger.error(s"tamtams : collection structure unknown (${thingRepo.collection} should be a collection of Things) ")
          InternalServerError
        }
      } recover { // deal with exceptions related to database connection
        DbExceptionResults
      }
    }
  }

}
