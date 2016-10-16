package database

import models.Thing
import play.modules.reactivemongo.ReactiveMongoApi


/**
  * Class to manage Things MongoDb Collection
  */
class ThingRepo(reactiveMongoApi: ReactiveMongoApi,
                collectionName: String = "thingsCollection") extends ObjectRepository[Thing] {
  import utils.ThingJsonConversion._
  import reactivemongo.play.json._
  import reactivemongo.api.indexes.IndexType.Geo2DSpherical
  import com.typesafe.config.ConfigFactory
  import play.api.libs.json.{JsArray, JsObject, Json}
  import reactivemongo.play.json.collection.JSONCollection
  import scala.concurrent.{ExecutionContext, Future}

  logger.debug("thingRepo collection read configuration" + collectionName)

  // define indexing on this collection
  val thingIdIndex: reactivemongo.api.indexes.Index = reactivemongo.api.indexes.Index(Seq(("position", Geo2DSpherical)))
  //todo check if def is ok or val better
  override def collection(implicit ec: ExecutionContext) = {
    reactiveMongoApi.database.map {
      connectedDb => connectedDb.collection[JSONCollection](collectionName)
    }.map {
      (col: JSONCollection) => {
        col.indexesManager.ensure(thingIdIndex)
        col
      }
    }
  }

  override def idFieldName : String = "thingId"

  override def repoToObj(repoObj: JsObject): Thing = geoJsonThingToJsThing(repoObj).as[Thing]

  override def objToRepo(obj: Thing): JsObject = thingToGeoJsonThing(obj)

  def upsertObject(obj: Thing)(implicit ec: ExecutionContext) =  super.upsertObject(obj, obj.thingId)(ec)

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
  def findNear(lon: Double,
               lat: Double,
               maxDistance : Option[Double],
               num : Option[Int])
              (implicit ec: ExecutionContext) : Future[Seq[JsObject]] = {

    import reactivemongo.api.commands.Command

    val defaultMaxDistance : Double = 5000000
    val defaultMaxNum : Int = 100

    val geoJsonPoint: JsObject = Json.obj(
      "type" -> "Point",
      "coordinates" -> Seq(lon, lat))

    val commandDoc =
      Json.obj(
        "geoNear" -> collectionName,
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
