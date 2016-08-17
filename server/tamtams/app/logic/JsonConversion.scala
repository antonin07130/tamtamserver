package logic
import play.api.libs.json._
import play.api.libs.functional.syntax._

// todo : object or class ? http://stackoverflow.com/questions/38668171/scala-define-implicit-functions-in-an-object-or-in-a-class
/**
  * Utility class defining implicit conversion functions
  * for [[Thing]] and [[User]] used by [[play.api.libs.json]]
  */
object JsonConversion {
  
  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[User]] to Json
    */
  implicit val userWrites = Json.writes[User]

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Position]] to Json
    */
  implicit val positionWrites = Json.writes[Position]

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
    * JSON reader and validation for [[User]]
    */
  implicit val userReads: Reads[User] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "age").read[Short]
    )(User.apply _)

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
    (JsPath \ "id").read[String] and
      (JsPath \ "pict").read[String] and
      (JsPath \ "description").read[String] and
      (JsPath \ "price").read[Price] and
      (JsPath \"position").read[Position] and
      (JsPath \"stuck").read[Boolean]
    )(Thing.apply _)
}
