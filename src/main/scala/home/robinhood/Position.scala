package home.robinhood

import spray.json._

object Position extends DefaultJsonProtocol {
    implicit object PositionJsonFormat extends RootJsonFormat[Position] {
        override def read(json: JsValue): Position = deserializationError("No need to deserialize Position")

        override def write(p: Position): JsValue = JsObject(
            "averageBuyPrice" -> JsNumber(p.averageBuyPrice),
            "quantity"        -> JsNumber(p.quantity),
            "heldForBuy"      -> JsNumber(p.heldForBuy),
            "heldForSell"     -> JsNumber(p.heldForSell)
        )
    }
}

case class Position(averageBuyPrice: Double, quantity: Int, heldForBuy: Int, heldForSell: Int)