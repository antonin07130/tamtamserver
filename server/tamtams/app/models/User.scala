package models


/**
  * User model implementation
  */
case class User(_id: String, interestedIn:Seq[String], sellingThings:Seq[Thing])

