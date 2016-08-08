package logic
import play.api.libs.json._
import play.api.libs.functional.syntax._

// todo : implicit functions should be declared in an object or in a class?
/**
  * Utility class defining implicit conversion functions
  * for [[Thing]] and [[User]] used by [[play.api.libs.json]]
  */
object JsonConversion {


  implicit val userWrites = new Writes[User] {
    def writes(user: User) = Json.obj(
      "id" -> user.id,
      "name" -> user.name,
      "age" -> user.age
    )
  }



  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Position]] to Json
    */
  implicit val locationWrites = new Writes[Position] {
    def writes(position: Position) = Json.obj(
      "lat" -> position.lat,
      "lon" -> position.lon
    )
  }

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Price]] to Json
    */
  implicit val priceWrites = new Writes[Price] {
    def writes(price: Price) = Json.obj(
      "currency" -> price.currency,
      "price" -> price.price
    )
  }

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[Thing]] to Json
    */
  implicit val thingWrites = new Writes[Thing] {
    def writes(thing: Thing) = Json.obj(
      "id" -> thing.id,
      "pict" -> thing.pict,
      "description" -> thing.description,
      "price" -> thing.price,
      "position"-> thing.position,
      "stuck" -> thing.stuck)
  }


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
