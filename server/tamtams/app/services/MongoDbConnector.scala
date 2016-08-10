package services

import java.time.{Clock, Instant}
import javax.inject._

import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

import reactivemongo.api.MongoConnection


/**
 * This class demonstrates how to run code when the
 * application starts and stops. It starts a MongoDb connection when the application starts.
 * When the application stops ...
 *
 * This class is registered for Guice dependency injection in the
 * [[Module]] class. We want the class to start when the application
 * starts, so it is registered as an "eager singleton". See the code
 * in the [[Module]] class to see how this happens.
 *
 * This class needs to run code when the server stops. It uses the
 * application's [[ApplicationLifecycle]] to register a stop hook (callback).
 */
/*
@Singleton
class MongoDbConnector @Inject()(appLifecycle: ApplicationLifecycle) {

  // This code is called when the application starts.
  Logger.info(s"tamtams : Initiating MongoDb Connection")
  val driver = new reactivemongo.api.MongoDriver

  // connect to a MongoDB server.
  val connection = driver.connection(List("localhost:27017"))

  Logger.info(s"tamtams : Initiating MongoDb Connection")


  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    Logger.info(s"tamtams : closing MongoDb conneciton mabe ? Or not....")
    driver.close()
    Future.successful(())
  }
}
*/