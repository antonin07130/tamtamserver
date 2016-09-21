package database

import com.typesafe.config.ConfigFactory
import models.Thing
import play.api.libs.json.{JsArray, JsNull, JsObject, Json}
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.WriteResult
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.api.indexes.IndexType._
import utils.ThingJsonConversion._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by antoninpa on 20/09/16.
  */
// todo add type parametrisation to this trait
abstract trait MongoRepository {
  import reactivemongo.play.json._

  def collection(implicit ec: ExecutionContext) : Future[JSONCollection]

   def upsert(selector : JsObject, obj: JsObject)(implicit ec: ExecutionContext): Future[WriteResult] =
    collection.flatMap(jscol => jscol.update(selector, obj, upsert = true))

   def remove(selector: JsObject)(implicit ec: ExecutionContext) : Future[WriteResult] =
    collection.flatMap(jscol => jscol.remove(selector))

   def find(selector: JsObject)(implicit ec: ExecutionContext): Future[Seq[JsObject]] =
    collection.flatMap(jscol => jscol.find(selector).cursor[JsObject]().collect[List]())

   def find()(implicit ec: ExecutionContext): Future[Seq[JsObject]] =
    collection.flatMap(col => col.find(JsObject(Seq(("", JsNull)))).cursor[JsObject]().collect[List]())

}


trait ObjectRepository[T] extends MongoRepository {

  /**
    * This function returns all objects of the collection
    * @param ec
    * @return a [[scala.concurrent.Future]] of [[scala.Seq]] of [[T]] objects
    */
  def findObjects()(implicit ec: ExecutionContext):Future[Seq[T]]

  /**
    * This function searches for objects with ids in idList
    * and returns them
    * @param idList list of object ids
    * @param ec
    * @return a [[scala.concurrent.Future]] of [[scala.Seq]] of [[T]] objects
    */
  def findObjects(idList: Seq[String])(implicit ec: ExecutionContext): Future[Seq[T]]

  /**
    * This function removes objects with ids in idList
    * @param idList list of object ids
    * @param ec
    * @return
    */
  def removeObjects(idList: Seq[String])(implicit ec: ExecutionContext):Future[WriteResult]

  /**
    * This function inserts the object in the collection or updates it (upsert semantics)
    * @param obj object to insert or update
    * @param ec
    * @return
    */
  def upsertObject(obj: T)(implicit ec: ExecutionContext): Future[WriteResult]
}




class ThingRepo(reactiveMongoApi: ReactiveMongoApi) extends ObjectRepository[Thing] {

  import utils.ThingJsonConversion._
  import reactivemongo.play.json._

  val thingsCollectionName = ConfigFactory.load().getString("mongodb.thingsCollection")

  // define indexing on this collection
  val thingIdIndex: reactivemongo.api.indexes.Index = reactivemongo.api.indexes.Index(Seq(("position", Geo2DSpherical)))
  //resolve this collection and make sure indexing is defined
  // todo check if executioncontext is needed here
  def thingsJSONCollection(implicit ec: ExecutionContext): Future[JSONCollection] =
  reactiveMongoApi.database.map(// once future database is completed :
    connectedDb => connectedDb.collection[JSONCollection](thingsCollectionName)
  ).map((col: JSONCollection) => {
    col.indexesManager.ensure(thingIdIndex)
    col
  }
  )

  //todo check if def is ok or val better
  override def collection(implicit ec: ExecutionContext) = thingsJSONCollection(ec)


  /**
    * This function adds a Thing to a collection
    * if not found this function creates a new Thing
    *
    * @param thing [[Thing]] object to add
    * @return [[Future]] of [[reactivemongo.api.commands.WriteResult]]
    */
  def upsertObject(thing: Thing)(implicit ec: ExecutionContext): Future[WriteResult] = {
    // GeoJson position : position {"type":"Point","coordinates":[lon,lat]}
    def geoJsonThing: JsObject = thingToGeoJsonThing(thing)
    def selector = Json.obj("thingId" -> thing.thingId)
    upsert(selector,geoJsonThing)
  }


  /**
    * Remove a [[Thing]] from a collection
    *
    * @param thingIds List of thingId of Things to delete
    * @return [[Future]] of [[reactivemongo.api.commands.WriteResult]]
    */
  def removeObjects(thingIds: Seq[String])(implicit ec: ExecutionContext)
  : Future[WriteResult] = {
    val selector = Json.obj("thingId" -> Json.obj("$in" -> (thingIds)))
    //logger.debug(s"tamtams : removing $thingId from database")
    remove(selector)
  }


  /**
    * Retrieve a Seq of Things from a mongoDb collection
    *
    * @param thingIds Seq of ThingIds to retrieve
    * @return a [[Seq]] of [[Thing]]s (that can be empty)
    */
  def findObjects(thingIds: Seq[String])(implicit ec: ExecutionContext) : Future[Seq[Thing]] = {
    val query = Json.obj("thingId" -> Json.obj("$in" -> (thingIds)))
    find(query).map(_.map(geoJsonThingToJsThing(_).as[Thing]))
  }


  /**
    * Retrieve a complete repository mongoDb collection
    * @deprecated : can return very large results
    * @return a [[Seq]] of [[Thing]]s (that can be empty)
    */
  override def findObjects()(implicit ec: ExecutionContext) : Future[Seq[Thing]] =
    find().map(_.map(geoJsonThingToJsThing(_).as[Thing]))


  /**
    * Retrieve [[Thing]]s around a geographical position
    * @param lon longitude
    * @param lat latitude
    * @param maxDistance maximum distance in meter
    * @param num maximum number results
    * @return JsObject representing an array of distance and thing :
    * [
    * {dis:x,obj:Thing1},
    * {dis:y,obj:Thing2},
    * ...
    * ]
    */
  def findNear(lon: Double,lat: Double, maxDistance : Option[Double], num : Option[Int])(implicit ec: ExecutionContext) : Future[Seq[JsObject]] = {

    import reactivemongo.api.commands.Command

      val defaultMaxDistance : Double = 5000000
      val defaultMaxNum : Int = 100

      val geoJsonPoint: JsObject = Json.obj(
        "type" -> "Point",
        "coordinates" -> Seq(lon, lat))

      val commandDoc =
        Json.obj(
          "geoNear" -> thingsCollectionName,
          "near" -> geoJsonPoint,
          "spherical" -> true,
          "minDistance" -> 0,
          "maxDistance" -> maxDistance.getOrElse[Double](defaultMaxDistance),
          "num" -> num.getOrElse[Int](defaultMaxNum))

      val runner = Command.run(JSONSerializationPack)

      val futureResult: Future[JsObject] = collection.flatMap { coll => {
        //logger.debug(s"tamtams : executing MongoCommand : $commandDoc")
        runner.apply(coll.db, runner.rawCommand(commandDoc)).one[JsObject]
      }
      }

      val futureJsGeoResult: Future[JsArray] =
        futureResult.map((jsAllResult: JsObject) => (jsAllResult \ "results").get.as[JsArray])

      def jsArrayToDisAndThing(mongoJsArray : JsArray): Seq[JsObject] =
        mongoJsArray.as[Seq[JsObject]]. // transform JsArray to Seq[JsObject]
          map {          // tranform each JsObject to (dis:JsObject,thing:JsObject)
          jsObj =>
            Json.obj(//todo : simplify this transformation
              "dis" -> (jsObj \ "dis").get.as[Double],
              "obj" -> geoJsonThingToJsThing((jsObj \ "obj").get.as[JsObject]).as[Thing] //as[Thing] to remove _id field
            )
        }

     futureJsGeoResult.map(jsArrayToDisAndThing)
     }


  }

/**
  * Connects to local database using [[database]] and
  * to TamtamThings collection as a [[reactivemongo.api.Collection]]
  * this is a def not a val because it must be re-evaluated at each call
  */
/*
// load collection names from configuration files
val usersCollectionName = ConfigFactory.load().getString("mongodb.usersCollection")
logger.debug(s"tamtams : reading collections from configuration : $thingsCollectionName and $usersCollectionName")




  /**
    * Connects to local database using [[database]] and
    * to TamtamUsers collection as a [[reactivemongo.api.Collection]]
    * this is a def not a val because it must be re-evaluated at each call
    */
  def usersJSONCollection: Future[JSONCollection] =
  database.map(// once future database is completed :
  connectedDb => connectedDb.collection[JSONCollection](usersCollectionName)
  )

  // register a callback on connection error :
  usersJSONCollection.onFailure {
  case _ => logger.error(s" tamtams : MongoDb connection for {$this} error ${PrimaryUnavailableException.message}")
}
  // register a callback on connection OK :
  usersJSONCollection.onSuccess {
  case _ => logger.debug(s" tamtams : MongoDb connection for {$this} OK")
}
*/