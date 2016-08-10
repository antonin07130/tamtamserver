package logic

import play.api.libs.functional.syntax._
import play.api.libs.json._

// todo : object or class ? http://stackoverflow.com/questions/38668171/scala-define-implicit-functions-in-an-object-or-in-a-class
/**
  * Utility class defining implicit conversion functions
  * for [[Thing]] and [[User]] used by [[play.api.libs.json.JsObject]]
  */
object JsonOConversion {

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[User]] to Json
    */
  implicit val userOWrites: OWrites[User] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "name").write[String] and
      (JsPath \ "age").write[Short]
    )(unlift(User.unapply))

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Position]] to Json
    */
  implicit val locationOWrites: OWrites[Position] = (
    (JsPath \ "lat").write[Double] and
      (JsPath \ "lon").write[Double]
    )(unlift(Position.unapply))



  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Price]] to Json
    */
  implicit val priceOWrites: OWrites[Price] = (
    (JsPath \ "currency").write[Short] and
      (JsPath \ "price").write[Float]
    )(unlift(Price.unapply))


  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Thing]] to Json
    */
  implicit val thingOWrites: OWrites[Thing] = (
    (JsPath \ "id").write[String] and
      (JsPath \ "pict").write[String] and
      (JsPath \ "description").write[String] and
      (JsPath \ "price").write[Price] and
      (JsPath \ "position").write[Position] and
      (JsPath \ "stuck").write[Boolean]
    )(unlift(Thing.unapply))


}
