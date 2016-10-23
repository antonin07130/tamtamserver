package controllers

import akka.util.Timeout
import database.{ThingRepo, UserRepo}
import models.User
import org.scalatest.GivenWhenThen
import play.api.libs.json.{JsDefined, _}
import play.api.test.Helpers._
import play.api.test._


//*****
import models.{Position, Price, Thing}
import utils.ThingJsonConversion._

///*************
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions

///***********

import org.scalatest._
import org.scalatestplus.play._
import play.api.inject.guice._
import play.api.{Application, Play}

///*****
/**
  * Created by antoninpa on 15/10/16.
  */

class ThingControllerSpecs extends PlaySpec with OneAppPerSuite with GivenWhenThen {

  val testDb = "mongodb://localhost/tamtamTestDb"
  val testCollection = "tamtamTestThings"

  // Override default parameters for test application :
  implicit override lazy val app : Application = {
    new GuiceApplicationBuilder()
      .configure(Map(
        "mongodb.thingsCollection" -> testCollection,
        "mongodb.uri" -> testDb))
      .build()
  }


  def repoFixture = {
    new {
      import scala.concurrent.Await

      val thingRepo = new ThingRepo(
        app.injector.instanceOf(classOf[ReactiveMongoApi]),
        app.configuration.getString("mongodb.thingsCollection").get)
      val userRepo = new UserRepo(
        app.injector.instanceOf(classOf[ReactiveMongoApi]),
        app.configuration.getString("mongodb.usersCollection").get)
      val testThing1 = Thing(
        "testThingId1",
        "PictureAsString", "createThing test object",
        Price(1, 1.1f),
        Position(40.0f, 40.0f),
        false)
      val testThing2 = Thing(
        "testThingId2",
        "PictureAsString", "createThing test object",
        Price(1, 1.1f),
        Position(40.0f, 40.0f),
        false)
      val testUser1 = User("testUser1")
      val testUser2 = User("testUser2")
      // utility functions
      def addToThingCollection(thing: Thing) = {
        thingRepo.upsertObject(thing)
      }

      def removeFromThingCollection(thingId: String) = {
        thingRepo.removeObjects(Seq(thingId))
      }
      def getFromThingCollection(thingId: String)(implicit timeout: Timeout): Thing = {
        Await.result(thingRepo.findObjects(Seq(thingId)), timeout.duration).head
      }
      def dropThingRepo(implicit timeout: Timeout) = {
        Await.result(thingRepo.collection, timeout.duration).drop(failIfNotFound = false)
      }
      def dropUserRepo(implicit timeout: Timeout) = {
        Await.result(userRepo.collection, timeout.duration).drop(failIfNotFound = false)
      }
    }
  }

  // todo : store all request results in an array and verify as a final test that all results have a json body

  "Routes" should {
    "send a 404 on a bad request" in {
      route(app, FakeRequest(GET, "/boum")).map(status(_)) mustBe Some(NOT_FOUND)
    }
  }


  "ThingController" when {
    "[DEPRECATED] Receiving a Thing creation request (PUT thing)" should {
      "Create a new Thing" in {
        val f = repoFixture
        val req = FakeRequest(PUT, "/things/" + f.testThing1.thingId).withJsonBody(Json.toJson(f.testThing1))
        val result = route(app, req).get

        result.andThen { case res => print(res) }

        //f.thingRepo.upsertObject(testThing)
        f.getFromThingCollection(f.testThing1.thingId) mustEqual f.testThing1
      }

      "Answer with a reachable URI in Location header" in {
        val f = repoFixture
        Given("a Thing object")
        When("a PUT request is sent with this object is sent to application")
        val req = FakeRequest(PUT, "/things/" + f.testThing1.thingId).withJsonBody(Json.toJson(f.testThing1))
        val result = route(app, req).get

        Then("a Location field in the response header must be sent back")
        header("Location", result) must not be empty

        And("Location field must contain an URI")
        header("Location", result) mustEqual Some(req.host + routes.ThingController.getThing(f.testThing1.thingId))

        When("using this URI")
        val req2 = FakeRequest(GET, header("location", result).get)
        val result2 = route(app, req2).get

        Then("the application must send back the initial object")
        contentAsJson(result2) mustEqual Json.toJson(f.testThing1)
      }

      "Answer with a json body containing a version of the header" in {
        val f = repoFixture
        Given("a Thing object")
        When("a PUT request with this object is sent to application")
        val req = FakeRequest(PUT, "/things/" + f.testThing1.thingId).withJsonBody(Json.toJson(f.testThing1))
        val result = route(app, req).get

        Then("response must contain a Json body")
        assert(contentAsJson(result) != JsNull, "response body does not contain a json value")

        And("the Json must contain a status key")
        (contentAsJson(result) \ "status") mustBe a [JsDefined]

        And("status key must contain status of the result")
        val body_status = (contentAsJson(result) \ "status").get.as[Int]
        assert(body_status == status(result), "status in header different from status in body json")
      }

      "Answer with CREATED when creating a new Thing" in {
        val f = repoFixture
        Given(s"a Thing collection with no ${f.testThing1.thingId} Thing in it")
        f.dropThingRepo

        When(s"requesting for insertion of a new thing with id : ${f.testThing1.thingId}")
        val req = FakeRequest(PUT, "/things/" + f.testThing1.thingId).withJsonBody(Json.toJson(f.testThing1))
        val result = route(app, req).get

        Then("result status must be CREATED")
        status(result) mustBe CREATED
      }

      "Answer with OK when updating an existing Thing" in{
        val f = repoFixture
        Given(s"a Thing collection with no ${f.testThing1.thingId} Thing in it")
        //removeFromThingCollection(f.testThing1.thingId)

        When(s"requesting for insertion of a new thing with id : ${f.testThing1.thingId}")
        val req = FakeRequest(PUT, "/things/" + f.testThing1.thingId).withJsonBody(Json.toJson(f.testThing1))
        val result = route(app, req).get

        Then("result status must be CREATED")
        status(result) mustBe OK
      }

      "cleanup after test serie" in {
        val f = repoFixture
        f.dropThingRepo
      }

    }

      "Receiving a sell thing request " should {
        "answer with NOT_FOUND if user does not exist" in{
          Given("an empty user repo")
          val f = repoFixture
          f.dropUserRepo

          When("a request is sent with an non existing user")
          val req = FakeRequest(PUT, "/users/"+ f.testUser1.userId + "/sellingThings/" + f.testThing1.thingId).withJsonBody(Json.toJson(f.testThing1))
          val result = route(app, req).get

          Then("result status must be NOT_FOUND")
          status(result) mustBe NOT_FOUND
        }
        "answer with forbidden if user is not logged in" in {
          assert(false,"TODO")
        }
        "answer with bad request if the request is not correct" in{
          val f = repoFixture
          f.userRepo.upsertObject(f.testUser1)
          Given("a malformed request")
          val req = FakeRequest(PUT, "/users/"+ f.testUser1.userId + "/sellingThings/" + f.testThing1.thingId)
            .withJsonBody(Json.toJson(Json.obj("key1" -> false, "key2" -> 0)))
          val result = route(app, req).get

          Then("result status must be BAD_REQUEST")
          status(result) mustBe BAD_REQUEST


          Given("another malformed request")
          val req2 = FakeRequest(PUT, "/users/"+ f.testUser1.userId + "/sellingThings/" + f.testThing1.thingId + "f")
            .withJsonBody(Json.toJson(f.testThing1))
          val result2 = route(app, req).get

          Then("result status must be BAD_REQUEST")
          status(result2) mustBe BAD_REQUEST
        }
        "add a new thing to user sellingThings array in user collection and to thing collection" in {
          assert(false,"TODO")

        }
        "answer with a reachable url in Location Header field" in {
          assert(false,"TODO")

        }
        "answer with a Json body containing a version of the header" in {
          assert(false,"TODO")

        }

      }

    /*
        "Receiving a removal request " should {
          "Remove an existing Thing" in {
            val req = FakeRequest(DELETE, "/things/" + f.testThing1.thingId)
            val result = route(app, req).get

            result.andThen { case res => print(res) }
            status(result) mustBe OK
          }
        }
    */
  }
}



