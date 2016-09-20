package database

import com.typesafe.config.ConfigFactory
import models.Thing
import play.api.libs.json.{JsNull, JsObject, Json}
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
trait PostRepository {
  def collection(implicit ec: ExecutionContext): Future[JSONCollection]

  def find()(implicit ec: ExecutionContext): Future[Seq[JsObject]]

  def find(selector: JsObject)(implicit ec: ExecutionContext): Future[Seq[JsObject]]

  def update(selector: JsObject, update: JsObject)(implicit ec: ExecutionContext): Future[WriteResult]

  def remove(selector: JsObject)(implicit ec: ExecutionContext): Future[WriteResult]

  def save(obj: JsObject)(implicit ec: ExecutionContext): Future[WriteResult]

}


class ThingMongoRepo(reactiveMongoApi: ReactiveMongoApi) extends PostRepository {

  import utils.ThingJsonConversion._
  import reactivemongo.play.json._

  val thingsCollectionName = ConfigFactory.load().getString("mongodb.thingsCollection")

  // define indexing on this collection
  val thingIdIndex: reactivemongo.api.indexes.Index = reactivemongo.api.indexes.Index(Seq(("position", Geo2DSpherical)))

  //resolve this collection
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
  override def collection(implicit ec: ExecutionContext): Future[JSONCollection] = thingsJSONCollection(ec)


  /**
    * This function adds a Thing to a collection
    * if not found this function creates a new Thing
    *
    * @param thing [[Thing]] object to add
    * @return [[Future]] of [[reactivemongo.api.commands.WriteResult]]
    */
  def addThingToThingsCollection(thing: Thing)(implicit ec: ExecutionContext): Future[WriteResult] = {

    // GeoJson position : position {"type":"Point","coordinates":[lon,lat]}
    def geoJsonThing: JsObject = thingToGeoJsonThing(thing)

    // selector used in our MongoDb request
    def selector = Json.obj("thingId" -> thing.thingId)
    // ask to write our User to the database
    collection.flatMap(jscol => jscol.update(selector, geoJsonThing, upsert = true))
  }

  override def save(obj: JsObject)(implicit ec: ExecutionContext): Future[WriteResult] =
    addThingToThingsCollection(obj.as[Thing])(ec)


  /**
    * Remove a [[Thing]] from a collection
    *
    * @param thingId thingId of Thing to delete
    * @return [[Future]] of [[reactivemongo.api.commands.WriteResult]]
    */
  def removeThingFromThingsCollection(thingId: String)(implicit ec: ExecutionContext)
  : Future[WriteResult] = {

    // selector used in our MongoDb request
    def selector = Json.obj("thingId" -> thingId)

    //logger.debug(s"tamtams : removing $thingId from database")
    remove(selector)
  }

  override def remove(selector: JsObject)(implicit ec: ExecutionContext): Future[WriteResult] =
    collection.flatMap(jscol => jscol.remove(selector))


  /**
    * Retrieve a Seq of Things from a mongoDb collection
    *
    * @param thingIds Seq of ThingIds to retrieve
    * @return a [[Seq]] of [[Thing]]s (that can be empty)
    */
  def getListOfThingsFromThingsCollection(thingIds: Seq[String])
  : Future[Seq[Thing]] = {

    val query = Json.obj("thingId" -> Json.obj("$in" -> (thingIds)))

    find(query).map(_.map(geoJsonThingToJsThing(_).as[Thing]))
  }

  override def find(selector: JsObject)(implicit ec: ExecutionContext): Future[Seq[JsObject]] = {
    def requestForThings(query: JsObject): Future[Seq[JsObject]] = {
      //logger.debug(s"tamtams : executing query in mongoDb : find ${query}")
      thingsJSONCollection.flatMap(jscol => jscol.find(query).cursor[JsObject]().collect[List]())
    }
    requestForThings(selector)
  }

  
  override def find()(implicit ec: ExecutionContext): Future[Seq[JsObject]] =
    collection.flatMap(col => col.find(JsObject(Seq(("", JsNull)))).cursor[JsObject]().collect[List]())

  def findAllThings : Future[Seq[Thing]] =
    find().map(_.map(geoJsonThingToJsThing(_).as[Thing])))


  /**
    * Retrieve a thing from a mongoDb collection
    *
    * @param thingId thingId of the [[Thing]] to find
    * @return an [[Option]] of [[Thing]] in the [[Future]]
    */
  def getThingFromThingsCollection(thingId: String)
  : Future[Option[Thing]] = {
    val findThingQuery: JsObject = Json.obj("thingId" -> thingId)
    val futureFindThing: Future[Option[Thing]] =
      thingsJSONCollection.flatMap(jscol => jscol.find(findThingQuery).one[JsObject].map {
        case Some(geoJsonThing) => Some(geoJsonThingToJsThing(geoJsonThing).as[Thing])
        case None => None
      })
    futureFindThing
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