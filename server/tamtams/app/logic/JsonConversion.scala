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
  implicit val userWrites: Writes[User] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "name").write[String] and
      (JsPath \ "age").write[Short]
    )(unlift(User.unapply))

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Position]] to Json
    */
  implicit val locationWrites: Writes[Position] = (
    (JsPath \ "lat").write[Double] and
      (JsPath \ "lon").write[Double]
    )(unlift(Position.unapply))

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Price]] to Json
    */
  implicit val priceWrites: Writes[Price] = (
    (JsPath \ "currency").write[Short] and
      (JsPath \ "price").write[Float]
    )(unlift(Price.unapply))

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Thing]] to Json
    */
  implicit val thingWrites: Writes[Thing] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "pict").write[String] and
      (JsPath \ "description").write[String] and
      (JsPath \ "price").write[Price] and
      (JsPath \ "position").write[Position] and
      (JsPath \ "stuck").write[Boolean]
    )(unlift(Thing.unapply))



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
