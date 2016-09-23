package database

import com.typesafe.config.ConfigFactory
import models.{Thing, User}
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{UpdateWriteResult, WriteResult}
import reactivemongo.play.json.collection.JSONCollection
import reactivemongo.api.indexes.IndexType._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try


/**
  * This traits implements most usefull and basic methods
  * to interface MongoDb collections.
  *
  * It only lacks a collection connection concrete definition.
  */
abstract trait MongoRepository {
  import reactivemongo.play.json._
  import play.api.Logger

  lazy val logger: Logger = Logger("application." + this.getClass())

  def collection(implicit ec: ExecutionContext) : Future[JSONCollection]

   def upsert(selector : JsObject, obj: JsObject)(implicit ec: ExecutionContext): Future[UpdateWriteResult] =
    collection.flatMap(jscol => jscol.update(selector, obj, upsert = true).
      andThen{case wr => {logger.debug(wr.get.toString)}})

   def remove(selector: JsObject)(implicit ec: ExecutionContext) : Future[WriteResult] =
    collection.flatMap(jscol => jscol.remove(selector).
      andThen{case wr => {logger.debug(wr.get.toString)}})

   def find(selector: JsObject)(implicit ec: ExecutionContext): Future[Seq[JsObject]] =
    collection.flatMap(jscol => jscol.find(selector).cursor[JsObject]().collect[List]())

   def find()(implicit ec: ExecutionContext): Future[Seq[JsObject]] =
    collection.flatMap(col => col.find(JsObject(Seq(("", JsNull)))).cursor[JsObject]().collect[List]())

}


/**
  * This Trait implements most usefull functions to deal
  * with MongoDb collections of objects.
  * @tparam T Type of objects stored by this repository
  */
trait ObjectRepository[T] extends MongoRepository {

  // name of the field containing Ids in MongoDb
  def idFieldName : String

  /**
    * Convert a JsObject stored in MongoDb to an object
    * of type [[T]] used in the application.
    * @param repoObj : [[JsObject]] stored in mongoDb
    * @return the conversion of the [[JsObject]] to [[T]]
    */
  def repoToObj(repoObj : JsObject) : T

  /**
    * Convert an obj of type [[T]] to a [[JsObject]] to store
    * in mongoDb database.
    * @param obj object to convert
    * @return converted object
    */
  def objToRepo(obj : T) : JsObject


  /**
    * This function returns all objects of the collection
    * @param ec
    * @return a [[scala.concurrent.Future]] of [[scala.Seq]] of [[T]] objects
    */
  def findObjects()(implicit ec: ExecutionContext) : Future[Seq[T]] =
    find().map(_.map(repoToObj(_)))

  /**
    * This function searches for objects with ids in idList
    * and returns them
    * @param idList list of object ids
    * @param ec
    * @return a [[scala.concurrent.Future]] of [[scala.Seq]] of [[T]] objects
    */
  def findObjects(idList: Seq[String])(implicit ec: ExecutionContext): Future[Seq[T]] = {
    val query =  Json.obj(idFieldName -> Json.obj("$in" -> (idList)))
    find(query).map(_.map(repoObj => repoToObj(repoObj)))
  }

  /**
    * This function removes objects with ids in idList
    * @param idList list of object ids
    * @param ec
    * @return
    */
  def removeObjects(idList: Seq[String])(implicit ec: ExecutionContext) : Future[WriteResult] = {
    val selector = Json.obj(idFieldName -> Json.obj("$in" -> (idList)))
    remove(selector)
  }

  /**
    * This function inserts the object in the collection or updates it (upsert semantics)
    * @param obj object to insert or update
    * @param ec
    * @return
    */
  def upsertObject(obj: T, id : String)(implicit ec: ExecutionContext): Future[WriteResult] = {
  def selector = Json.obj(idFieldName -> id)
  upsert(selector,objToRepo(obj))
  }
}






