import controllers.routes
import org.scalatest.{BeforeAndAfter, GivenWhenThen}
import org.scalatestplus.play._
import play.api.libs.json
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._


//*****
import models.Thing
import models.{Position, Price}
import utils.ThingJsonConversion._
///*************
import javax.inject._
import controllers.ThingController
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
///***********

import org.scalatest._
import org.scalatestplus.play._
import play.api.{Play, Application}
import play.api.inject.guice._

///*****
/**
  * Created by antoninpa on 15/10/16.
  */

class ThingControllerSpecs extends PlaySpec with OneAppPerTest with GivenWhenThen {

  val testDb = "mongodb://localhost/tamtamTestDb"
  val testCollection = "tamtamTestThings"

  // Override newAppForTest to create a FakeApplication with other than non-default parameters.
  implicit override def newAppForTest(testData: TestData): Application =
  new GuiceApplicationBuilder()
    .configure(Map(
      "mongodb.thingsCollection" -> testCollection,
      "mongodb.uri" -> testDb))
    .build()

  "The OneAppPerTest trait" must {
    "provide an Application" in {
      app.configuration.getString("mongodb.thingsCollection") mustBe Some(testCollection)
    }
    "make the Application available implicitly" in {
      def getConfig(key: String)(implicit app: Application) = app.configuration.getString(key)
      getConfig("mongodb.thingsCollection") mustBe Some(testCollection)
    }
    "start the Application" in {
      Play.maybeApplication mustBe Some(app)
    }
  }

  
  /*
  with BeforeAndAfter

  val builder = new StringBuilder
  val buffer = new ListBuffer[String]

  before {
    builder.append("ScalaTest is ")
  }

  after {
    builder.clear()
    buffer.clear()
  }
  */

  "Routes" should {
    "send a 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }
  }


  "ThingController" when {
    "Receiving a Creation request" should {
      "Create a new Thing" in {
        val testThingId = "createTestId"
        val testThing = Thing(testThingId,"PictureAsString","createThing test object", Price(1, 1.1f), Position(40.0f, 40.0f),false)
        val req = FakeRequest(PUT, "/things/"+ testThingId).withJsonBody(Json.toJson(testThing))
        val result = route(app, req).get

        result.andThen{case e => print(e)}
        status(result) mustBe CREATED
      }

      "Answer with a reachable URI in Location header" in {
        Given ("a Thing object")
        val testThingId = "createTestId"
        val testThing = Thing(testThingId,"PictureAsString","createThing test object", Price(1, 1.1f), Position(40.0f, 40.0f),false)

        When ("a PUT request is sent with this object is sent to application")
        val req = FakeRequest(PUT, "/things/"+ testThingId).withJsonBody(Json.toJson(testThing))
        val result = route(app, req).get

        Then ("a Location field in the response header must be sent back")
        header("Location",result) must not be empty

        And ("Location field must contain an URI")
        header("Location",result) mustEqual  Some(req.host + routes.ThingController.getThing(testThingId))




        When("using this URI")
        val req2 = FakeRequest(GET, header("location",result).get)
        val result2 = route(app, req2).get

        Then("the application must send back the initial object")
        contentAsJson(result2) mustEqual Json.toJson(testThing)
      }

      "Answer with a json body containing a version of the header" in {
        Given("a Thing object")
        val testThingId = "createTestId"
        val testThing = Thing(testThingId, "PictureAsString", "createThing test object", Price(1, 1.1f), Position(40.0f, 40.0f), false)

        When("a PUT request is sent with this object is sent to application")
        val req = FakeRequest(PUT, "/things/" + testThingId).withJsonBody(Json.toJson(testThing))
        val result = route(app, req).get

        Then ("response must contain a Json body")
        assert(contentAsJson(result) != JsNull)
      }

    }
  }

}


