package database


import play.api.libs.json.{JsObject, Json}
import reactivemongo.api.SerializationPack
import reactivemongo.api.commands.{Command, CommandWithPack, CommandWithResult, ImplicitCommandHelpers}
import reactivemongo.api.Collection


/**
  * This specifies what is the command input (arguments),
  * and what kind of result will be deserialized from the output,
  * using the trait CommandWithResult[CustomResult].
  *
  * If the command returns a document and you want to directly get that,
  * it can be specified with CommandWithResult[pack.Document].
  */

trait GeoNearCommand[P <: SerializationPack] extends ImplicitCommandHelpers[P] {
  case class GeoNear(
                     collection: Collection,
                     query: pack.Document) extends Command
    with CommandWithPack[pack.type] with CommandWithResult[GeoNearResult]

  case class GeoNearResult(waitedMS: Int, results: List[(Double,pack.Document)], stats : pack.Document)
}



import reactivemongo.api.BSONSerializationPack
import reactivemongo.play.json.JSONSerializationPack

object JSONGeoNearCommand extends GeoNearCommand[JSONSerializationPack.type] {
  val pack = JSONSerializationPack

  object Implicits {
    import reactivemongo.play.json.JSONSerializationPack.{Reader,Writer,Document}
    /*
    import reactivemongo.bson.{
    BSONDocument, BSONDocumentReader, BSONDocumentWriter, BSONNumberLike
    }
*/
    implicit object BSONWriter extends Writer[GeoNear] {
      def write(geoNear: GeoNear): Document = {
        // { "custom": name, "query": { ... } }
  //      BSONDocument("custom" -> geoNear.name, "query" -> custom.query)
        Json.obj("geoNear" -> "TamtamThings",
            "near" -> geoJsonPoint,
            "spherical" -> true,
            "minDistance" -> 0,
            "maxDistance" -> 5000
          )

      }
    }


    implicit object JSONReader extends Reader[GeoNearResult] {
      def read(result: JsObject): GeoNearResult = (for {
        count <- result.getAs[BSONNumberLike]("count").map(_.toInt)
        matching <- result.getAs[List[String]]("matching")
      } yield GeoNearResult(count, matching)).get
    }
  }
}

