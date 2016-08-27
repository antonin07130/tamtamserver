package utils

import models.{User}
import ThingJsonConversion._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Utility class defining implicit conversion functions
  * for [[User]] used by [[play.api.libs.json]]
  */
object UserJsonConversion {

  /**
    * This helper function helps [[play.api.libs.json]]
    * converting [[User]] to Json
    */
  implicit val userWrites = Json.writes[User]


  /**
    * JSON reader and validation for [[User]]
    */
  implicit val userReads: Reads[User] = (
    (JsPath \ "userId").read[String] and
      (JsPath \ "interestedIn").read[Seq[String]] and
      (JsPath \ "sellingThings").read[Seq[String]]
    )(User.apply _)
}
