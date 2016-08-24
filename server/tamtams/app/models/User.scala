package models


/**
  * User model implementation
  */
case class User(userId: String, interestedIn: Seq[String], sellingThings: Seq[Thing])

