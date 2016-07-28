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
  *
  * @param thingId raw id to be checked for correctness
  */
class thingId(thingId : String){
  val THINGIDMAXLENGTH = 20
  require(thingId.length < (THINGIDMAXLENGTH + 1), s"thingId length must be less than THINGIDMAXLENGTH")
}

import java.util.Base64
/**
  * this class represents a "thing"
  * @param id
  * @param pict
  * @param description
  * @param price
  * @param position
  * @param stuck
  */
case class Thing(id:String, pict, description: String, price : Price, position: Position , stuck : Boolean )
