package utils

import models.{Position, Price, Thing, User}
import play.api.libs.json.{JsPath, Json, OWrites, Reads}
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
    * converting [[Thing]] to Json
    */
  implicit val thingWrites = Json.writes[Thing]





  /**
    * JSON reader and validation for [[Position]]
    */
  implicit val locationReads: Reads[Position] = (
    (JsPath \ "lat").read[Double] and
      (JsPath \ "lon").read[Double]
    )(Position.apply _)

  /**
    * JSON reader and validation for [[Price]]
    */
  implicit val priceReads: Reads[Price] = (
    (JsPath \ "currency").read[Short] and
      (JsPath \ "price").read[Float]
    )(Price.apply _)

  /**
    * JSON reader and validation for [[Thing]]
    */
  implicit val thingReads: Reads[Thing] = (
    (JsPath \ "thingId").read[String] and
      (JsPath \ "pict").read[String] and
      (JsPath \ "description").read[String] and
      (JsPath \ "price").read[Price] and
      (JsPath \"position").read[Position] and
      (JsPath \"stuck").read[Boolean]
    )(Thing.apply _)
}
