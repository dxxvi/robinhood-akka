package home.robinhood

import spray.json._

import scala.util.Try

object RobinhoodPosition extends DefaultJsonProtocol {
    implicit object RobinhoodPositionJsonFormat extends RootJsonFormat[RobinhoodPosition] with Utils {
        override def read(json: JsValue): RobinhoodPosition = {
            val fields = json.asJsObject("Unable to call asJsObject on RobinhoodPosition jsValue").fields
            val averageBuyPrice = fields.get("average_buy_price") collect {
                case JsString(s) => Try(s.toDouble) getOrElse Double.NaN
            } getOrElse Double.NaN
            val instrument = fields.get("instrument").collect({ case JsString(s) => s}).get
            val quantity = fields.get("quantity").collect({ case JsString(s) => s.toDouble.toInt }).get
            val heldForBuy = fields.get("shares_held_for_buys").collect({ case JsString(s) => s.toDouble.toInt }).get
            val heldForSell = fields.get("shares_held_for_sells").collect({ case JsString(s) => s.toDouble.toInt }).get
            RobinhoodPosition(averageBuyPrice, instrument, quantity, heldForBuy, heldForSell)
        }

        override def write(obj: RobinhoodPosition): JsValue = serializationError("No need to serialize RobinhoodPosition")
    }
}

case class RobinhoodPosition(averageBuyPrice: Double, instrument: String, quantity: Int,
                             heldForBuy: Int, heldForSell: Int)