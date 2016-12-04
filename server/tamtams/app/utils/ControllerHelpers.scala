package utils

import play.api.http.{HttpEntity, Writeable}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Results.Status
import play.api.mvc._

/**
  * Created by antoninpa on 15/10/16.
  */
object ControllerHelpers {


  def resultWithJsonBody(status: Status, message: String) = {
    val body = Json.obj(
      "status" -> status.header.status,
      "message" -> message
    )
    status.apply(body)
  }
}
