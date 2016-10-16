package database

import models.User
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.UpdateWriteResult


/**
  * Created by antoninpa on 23/09/16.
  */
class UserRepo(reactiveMongoApi: ReactiveMongoApi,
               collectionName: String = "usersCollection") extends ObjectRepository[User] {
  import utils.UserJsonConversion
  import reactivemongo.play.json._
  import play.api.libs.json.{JsObject, JsValue, Json}
  import reactivemongo.api.commands.WriteResult
  import reactivemongo.play.json.collection.JSONCollection
  import scala.concurrent.{ExecutionContext, Future}
  import com.typesafe.config.ConfigFactory

  logger.debug(s"tamtams : userRepo collection read configuration : $collectionName")

  /**
    * Connects to local database using [[database]] and
    * to TamtamUsers collection as a [[reactivemongo.api.Collection]]
    * this is a def not a val because it must be re-evaluated at each call
    */
  //todo check if def is ok or val better
  override def collection(implicit ec: ExecutionContext) =   reactiveMongoApi.database.map(// once future database is completed :
    connectedDb => connectedDb.collection[JSONCollection](collectionName)
  )


  override def objToRepo(obj: User): JsObject = UserJsonConversion.userWrites.writes(obj)

  override def repoToObj(repoObj: JsObject): User = UserJsonConversion.userReads.reads(repoObj).get

  override def idFieldName: String = "userId"

  def upsertObject(obj: User)(implicit ec: ExecutionContext) =  super.upsertObject(obj, obj.userId)(ec)

  /**
    * Add a newValue to an array of [[String]]s within a user object
    * in a collection containing users
    *
    * @param userId        : id of the user whose array of things will be updated
    * @param userArrayName : name as [[String]] of the array to update
    * @param newValue      : value as [[String]] to insert in the array
    * @return [[Future]] of the [[WriteResult]] of this operation
    */
  def addValueToUserArray(userId: String, userArrayName: String, newValue: String)(implicit ec: ExecutionContext)
  : Future[Int] = {
    def selector = Json.obj("userId" -> userId)
    def insertionRequest = Json.obj("$addToSet" -> Json.obj(userArrayName -> newValue))

    logger.debug(s"mongo update command: $selector $insertionRequest")
    collection.flatMap(jscol => jscol.update(selector, insertionRequest, upsert = false)).map{
      case UpdateWriteResult(true,0,_,_,_,None,None,None) => 0
      case UpdateWriteResult(true,1,nModified,_,_,None,None,None) => nModified
      case _ => throw new IllegalStateException("addValueToUserArray()")
    }
  }

  /**
    * remove a valueToRemove from an array of [[String]]s within a user object
    * in a collection containing users
    *
    * @param userId        : id of the user whose array of things will be updated
    * @param userArrayName : name as [[String]] of the array to update
    * @param valueToRemove : value as [[String]] to remove from the array
    * @return [[Future]] of number of modified objects
    */
  def removeValueFromUserArray(userId: String,
                               userArrayName: String,
                               valueToRemove: String)
                              (implicit ec: ExecutionContext):Future[Int] = {
    def selector = Json.obj("userId" -> userId)
    // define a mongoDb request to remove the value from the userArray
    def removalRequest = Json.obj("$pull" -> Json.obj(userArrayName -> valueToRemove))

    // query the database
    collection.flatMap(jscol => {
      //logger.debug(s"tamtams : executing query in mongoDb : update ${selector}, request: ${removalRequest}")
      jscol.update(selector, removalRequest)
    }).map{
      case UpdateWriteResult(true,0,_,_,_,None,None,None) => 0 // doc no found
      case UpdateWriteResult(true,1,nModified,_,_,None,None,None) => nModified // doc found
      case _ => throw new IllegalStateException("removeValueFromUserArray()")
    }.andThen{case _ => logger.debug("mongo update command: " + selector + " " + removalRequest)}
  }


  def getArrayFromUser(userId: String, userArrayName: String)(implicit ec: ExecutionContext)
  : Future[Option[Seq[String]]] = {
    val findUserQuery: JsObject = Json.obj("userId" -> userId)
    val projectOnlyArray: JsObject = Json.obj(userArrayName -> 1, "_id" -> 0)

    // try to get a Js value containing an array named "userArrayName"
    val jsonAr: Future[Option[JsValue]] = {
      logger.debug(s"tamtams : executing query in mongoDb : find ${findUserQuery}, projection : ${projectOnlyArray}")
      collection.flatMap(jscol => jscol.find(findUserQuery, projectOnlyArray).one[JsValue])
    }

    // try to extract the array and store it as a Seq[String]
    jsonAr.flatMap {
      case Some(jsonVal) => {
        val ar = (jsonVal \ userArrayName).as[Seq[String]]
        logger.debug(s"tamtams : returning following array from MongoDb : ${ar}")
        Future.successful(Some(ar))
      }
      case None => {
        val msg = s"tamtams : no array named ${userArrayName} found in mongoDb for ${userId}"
        logger.debug(msg)
        Future.failed(throw new NoSuchElementException(msg))
      }
    }
  }

}
