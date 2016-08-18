package models


/**
  * User model implementation
  */
case class User(id: String, interestedIn:Seq[String], sellingThings:Seq[Thing])

