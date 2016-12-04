package models


/**
  * User model implementation
  */
case class User(userId: String, interestedIn: Seq[String] = List(), sellingThings: Seq[String] = List())

