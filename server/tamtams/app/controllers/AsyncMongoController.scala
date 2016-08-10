package controllers

import javax.inject.Inject

import scala.concurrent.Future

import logic.JsonOConversion._
import logic.JsonConversion._
import logic.Thing

//todo check if these imports are necessary :
import play.api.Logger
import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._



// Reactive Mongo imports
import reactivemongo.api.Cursor

import play.modules.reactivemongo.{ // ReactiveMongo Play2 plugin
MongoController,
ReactiveMongoApi,
ReactiveMongoComponents
}

import play.modules.reactivemongo.json._, ImplicitBSONHandlers._


// BSON-JSON conversions/collection
import reactivemongo.play.json._
import play.modules.reactivemongo.json.collection._

/**
  * Created by antoninpa on 8/10/16.
  */
class AsyncMongoController @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents {

  /*
   * Get a JSONCollection (a Collection implementation that is designed to work
   * with JsObject, Reads and Writes.)
   * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
   * the collection reference to avoid potential problems in development with
   * Play hot-reloading.
   */
  def collection: JSONCollection = db.collection[JSONCollection]("persons")

  def createThingAsync(thingId: String) = Action.async(parse.json[Thing]) {request =>
    Logger.info(request.body.toString())

    collection.insert(request.body).map(lastError =>
      Ok("Mongo LastError: %s".format(lastError)))
  }


}
