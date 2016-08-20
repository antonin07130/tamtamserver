package models
import java.util.Currency

import scala._


/**
  * This class represents a price and its currency.
  * @param currency : iso4217 code for this currency
  * @param price : price in the defined currency
  */
case class Price(currency: Short, price : Float)

/**
  * this class represents a position
  * @param lat latitude value
  * @param lon longitude value
  */
case class Position(lat : Double, lon : Double)


// todo : verify if parameters validation should happen here using specific types
/**
  * This class represents a thing which are real life objects exchanged in tamtam
  * @param _id unique id of this thing composed by
  * @param pict the picture encoded in base64 stored in a String
  * @param description a text description of the object completed by the seller
  * @param price the price of the thing
  * @param position the current known position of the thing
  * @param stuck true iff the thing is moving with the seller
  */
case class Thing(_id: String, pict : String, description: String, price : Price, position: Position , stuck : Boolean )
