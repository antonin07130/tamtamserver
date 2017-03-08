package utils

import models._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Utility class defining implicit conversion functions
  * for [[Thing]] used by [[play.api.libs.json]]
  */
object ThingJsonConversion {

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Position]] to Json
    */
  implicit val positionWrites: OWrites[Position] = Json.writes[Position]

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Price]] to Json
    */
  implicit val priceWrites= Json.writes[Price]

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Picture]] to Json
    */
  implicit val pictureWrites= Json.writes[Picture]

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Thing]] to Json
    */
  implicit val thingWrites = Json.writes[Thing]



  /**
    * JSON reader and validation for [[Position]]
    */
  implicit val locationReads: Reads[Position] = (
    (JsPath \ "lon").read[Double] and
      (JsPath \ "lat").read[Double]
    )(Position.apply _)

  /**
    * JSON reader and validation for [[Price]]
    */
  implicit val priceReads: Reads[Price] = (
    (JsPath \ "currency").read[String] and
      (JsPath \ "price").read[Float]
    )(Price.apply _)

  /**
    * JSON reader and validation for [[Picture]]
    */
  implicit val pictureReads: Reads[Picture] = (
    (JsPath \ "pictureId").read[String] and
      (JsPath \ "pictureData").read[String]
    )(Picture.apply _)

  /**
    * JSON reader and validation for [[Thing]]
    */
  implicit val thingReads: Reads[Thing] = (
    (JsPath \ "thingId").read[String] and
      (JsPath \ "pict").read[Picture] and
      (JsPath \ "description").read[String] and
      (JsPath \ "price").read[Price] and
      (JsPath \"position").read[Position] and
      (JsPath \"stuck").read[Boolean]
    )(Thing.apply _)


  /**
    * Helper function to convert a [[Thing]] to a JsObject having
    * position encoded in GeoJson
    * @param thing
    * @return
    */
  def thingToGeoJsonThing(thing: Thing): JsObject = {
    // GeoJson position : position {"type":"Point","coordinates":[lon,lat]}
    val geoJsonPosition: JsObject = Json.obj(
      "position" -> Json.obj(
        "type" -> "Point",
        "coordinates" -> Seq(thing.position.lon, thing.position.lat)))
    // convert our thing to Json (using standards implicit writers)
    val thingJson: JsObject = Json.toJson(thing).as[JsObject]
    // merge thing json and geoJson JsObject. That updates position key with our new geoJson position
    thingJson ++ geoJsonPosition
  }

  /**
    * Helper function to convert JsObject representations of [[Thing]]
    * @param geoJsonThing a [[JsObject]] representing a
    * [[Thing]] with position encoded in GeoJson :
    * GeoJson : {position : {"type" : "Point", "coordinates" : [lon, lat]}}
    * @return [[JsObject]] representing a [[Thing]] with position encoded
    * as Json position : {"position" : { "lon": lon, "lat":lat }}
    */
  // helper function to convert position node from GeoJson to Json
  // GeoJson position : position {"type":"Point","coordinates":[lon,lat]}
  // Json position : {"position" : { "lon": lon, "lat":lat }}
  def geoJsonThingToJsThing(geoJsonThing: JsObject): JsObject = {
    val position: Position = Position(
      (geoJsonThing \ "position" \ "coordinates") (0).as[Double],
      (geoJsonThing \ "position" \ "coordinates") (1).as[Double]
    )
    val jsonPos = Json.obj("position" -> position)
    geoJsonThing ++ jsonPos
  }

}
