/**
  * Created by antoninpa on 07/10/16.
  */
import javax.inject._

import akka.io.Tcp.Message
import play.api.http.DefaultHttpErrorHandler
import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.routing.Router

import scala.concurrent._
import utils.ControllerHelpers.resultWithJsonBody

@Singleton
class ErrorHandler @Inject() (
                               env: Environment,
                               config: Configuration,
                               sourceMapper: OptionalSourceMapper,
                               router: Provider[Router]
                             ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {



  override def onDevServerError(request: RequestHeader, exception: UsefulException) ={
    Future.successful(
      resultWithJsonBody(InternalServerError,
        "A dev server error occurred : " + exception.getMessage)
    )
  }

  override def onProdServerError(request: RequestHeader, exception: UsefulException) = {
    Future.successful(
      resultWithJsonBody(InternalServerError,
        "A prod server error occurred, please contact US ! : " + exception.getMessage)
    )
  }

  override def onForbidden(request: RequestHeader, message: String) = {
    Future.successful(
      resultWithJsonBody(Forbidden,"You're not allowed to access this resource : " + message)
    )
  }

  override def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(
      resultWithJsonBody(BadRequest,"Bad Request : " + message)
    )
  }
}