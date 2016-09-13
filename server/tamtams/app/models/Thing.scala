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
  * to prepare GeoJson std, the order of positions should be lon, lat, [altitude]
  * @param lon latitude value
  * @param lat longitude value
  */
case class Position(lon : Double, lat : Double)


// todo : verify if parameters validation should happen here using specific types
/**
  * This class represents a thing which are real life objects exchanged in tamtam
 *
  * @param thingId unique id of this thing composed by
  * @param pict the picture encoded in base64 stored in a String
  * @param description a text description of the object completed by the seller
  * @param price the price of the thing
  * @param position the current known position of the thing
  * @param stuck true iff the thing is moving with the seller
  */
case class Thing(thingId: String, pict: String, description: String, price: Price, position: Position, stuck: Boolean)
