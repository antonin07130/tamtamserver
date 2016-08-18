package models

import play.api.Logger

/**
  * This object is used for testing purpose
  * it contains 3 [[Thing]]s
  * thing1 : id = idThing1
  * thing2 : id = idThing2
  * thing3 : id = idThing3
  *
  * a Seq[[Thing]] containing these 3 [[Thing]]s
  * thingSeq
  *
  * And a thing generator that returns
  * a random [[Thing]].
  */
object ThingsGenerator {

  /** known [[Thing]] for testing purpose **/
  val thing1 : Thing = Thing("idThing1",
    "AAaaaIaAMaBASEa64aENCODEDaaaag==",
    "cest un premier truc",
    Price("489".toShort,10),
    Position(10.toDouble,10.toDouble),
    false)

  /** known [[Thing]] for testing purpose **/
  val thing2 : Thing = Thing("idThing2",
    "BBbbbIaAMaBASEa64aENCODEDbbbbg==",
    "cest un deuxieme truc",
    Price("489".toShort,20),
    Position(20.toDouble,20.toDouble),
    false)

  /** known [[Thing]] for testing purpose **/
  val thing3 : Thing = Thing("idThing3",
    "AAaaaIaAMaBASEa64aENCODEDaaaag==",
    "cest un troisieme truc",
    Price("489".toShort,30),
    Position(30.toDouble,30.toDouble),
    false)

  /** known List of [[Thing]] for testing purpose **/
  val thingSeq : Seq[Thing] = Seq(thing1,thing2,thing3)

  /** random [[Thing]] generator for testing purpose **/
  def generate : Thing = {
    Thing("id"+scala.util.Random.alphanumeric.take(5).mkString,
      scala.util.Random.alphanumeric.take(5).mkString+
        "IAMBASE64"+
        scala.util.Random.alphanumeric.take(5).mkString,
      "une description precise",
      Price("489".toShort,scala.util.Random.nextFloat()),
      Position(scala.util.Random.nextDouble(),scala.util.Random.nextDouble()),
      scala.util.Random.nextBoolean())
  }

  Logger.info(s"tamtams : Seq of Things created  : $thingSeq .")

}
