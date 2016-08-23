package models


/**
  * User model implementation
  */
case class User(idUser: String, interestedIn: Seq[String], sellingThings: Seq[Thing])

