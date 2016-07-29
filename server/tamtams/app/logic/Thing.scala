package logic
import java.util.Currency

import scala._


/**
  * This class represents a price and its currency.
  * @param currency : iso4217 code for this currency
  * @param price : price in the defined currency
  */
class Price(currency: Byte, price : Float)

/**
  * this class represents a position
  * @param lat latitude value
  * @param lon longitude value
  */
class Position(lat : Double, lon : Double)

/**
  * the specific string used for Thing ids
  * @param thingId raw id to be checked for correctness
  */
class IdThing(thingId : String){
  val THINGIDMAXLENGTH = 20
  require(thingId.length < (THINGIDMAXLENGTH + 1), s"thingId length must be less than THINGIDMAXLENGTH")
}

import java.util.Base64

/**
  * This class represents a thing which are real life objects exchanged in tamtam
  * @param id unique id of this thing composed by
  * @param pict the picture encoded in base64 stored in an array of byte
  * @param description a text description of the object completed by the seller
  * @param price the price of the thing
  * @param position the current known position of the thing
  * @param stuck true iff the thing is moving with the seller
  */
case class Thing(id:String, pict : Array[Byte], description: String, price : Price, position: Position , stuck : Boolean )
