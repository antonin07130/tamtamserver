package controllers

import javax.inject.Inject

import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
// business logic code :
import logic.JsonOConversion._
import logic.JsonConversion._
import logic.Thing

// play imports
import play.api.Logger
import play.api.mvc._
import play.api.libs.json._


// Reactive Mongo imports
import play.modules.reactivemongo._
import reactivemongo.api.ReadPreference
import reactivemongo.play.json._
import reactivemongo.play.json.collection._


/**
  * Created by antoninpa on 8/10/16.
  */
class AsyncMongoController @Inject()(val reactiveMongoApi: ReactiveMongoApi)
                                    (implicit exec: ExecutionContext)
  extends Controller with MongoController with ReactiveMongoComponents {


  val futureTestCollection : Future[JSONCollection] =
    database.map( // once future database is completed :
      connectedDb => connectedDb.collection[JSONCollection]("TamtamThings")
    )

  def createThingAsync(thingId: String) = Action.async(parse.json[Thing]) {
    request => {
      // todo : deal with all error cases (futureTestCollection, insert...)
      val futureResult : Future[WriteResult] = for {
        testCollection <- futureTestCollection //equivalent to a map
        lastError <- testCollection.insert(request.body)
      }yield {
        lastError
      }

      futureResult.map{
        case writeResult: WriteResult => Ok("Mongo LastError: %s".format(writeResult))
        case _ => InternalServerError
      }

    }
  }
//.getOrElse(Future.successful(BadRequest("invalid json"))
}
