/**
  * Created by antoninpa on 07/10/16.
  */
import javax.inject._

import play.api.http.DefaultHttpErrorHandler
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router
import scala.concurrent._

import play.api.libs.json._

@Singleton
class ErrorHandler @Inject() (
                               env: Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: Provider[Router]
                             ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    Future.successful(
      InternalServerError(Json.toJson("A server error occurred: " + exception.getMessage))
    )
  }

  override def onForbidden(request: RequestHeader, message: String) = {
    Future.successful(
      Forbidden(Json.toJson("You're not allowed to access this resource."))
    )
  }
}