package home.robinhood

import java.time.LocalDateTime

import spray.json._

import scala.util.Try

object RobinhoodOrder extends DefaultJsonProtocol {
    implicit object RobinhoodOrderJsonFormat extends RootJsonFormat[RobinhoodOrder] with Utils {
        override def read(json: JsValue): RobinhoodOrder = {
            val fields = json.asJsObject("Unable to call asJsObject on RobinhoodOrder jsValue").fields
            val updatedAt = fields.get("updated_at") collect {
                case JsString(s) => Try(utcString2LocalDateTime(s)).getOrElse(LocalDateTime.MIN)
            } getOrElse LocalDateTime.MIN
            val fees = fields.get("fees") collect {
                case JsString(s) => Try(s.toDouble).getOrElse(Double.NaN)
            } getOrElse Double.NaN
            val id = fields.get("id").collect({
                case JsString(s) => s
            }).get
            val cumulativeQuantity = fields.get("cumulative_quantity").collect({
                case JsString(s) => s.toDouble.toInt
            }).get
            val instrument = fields.get("instrument").collect({
                case JsString(s) => s
            }).get
            val state = fields.get("state").collect({
                case JsString(s) => s
            }).get
            val price = fields.get("price").collect({
                case JsString(s) => s.toDouble
            }).get
            val createdAt = fields.get("created_at") collect {
                case JsString(s) => Try(utcString2LocalDateTime(s)).getOrElse(LocalDateTime.MIN)
            } getOrElse LocalDateTime.MIN
            val side = fields.get("side").collect({
                case JsString(s) => s
            }).get
            val averagePrice = fields.get("price").collect({
                case JsString(s) => Try(s.toDouble) getOrElse Double.NaN
                case JsNull => Double.NaN
            }).get
            val quantity = fields.get("quantity").collect({
                case JsString(s) => s.toDouble.toInt
            }).get
            val account = fields.get("account").collect({
                case JsString(s) => s
            }).get
            val executions = fields.get("executions").map(_.convertTo[Array[RobinhoodOrderExecution]])
                    .getOrElse(Array.empty[RobinhoodOrderExecution])
            RobinhoodOrder(updatedAt, fees, id, cumulativeQuantity, instrument, state, price, createdAt, side,
                averagePrice, quantity, account, executions)
        }

        override def write(obj: RobinhoodOrder): JsValue = serializationError("No need to serialize RobinhoodOrder")
    }
}

case class RobinhoodOrder(
                                 updatedAt: LocalDateTime,
                                 fees: Double,
                                 id: String,
                                 cumulativeQuantity: Int,
                                 instrument: String,
                                 state: String,
                                 price: Double,
                                 createdAt: LocalDateTime,
                                 side: String,
                                 averagePrice: Double,
                                 quantity: Int,
                                 account: String,  // account url needed to make an order
                                 executions: Array[RobinhoodOrderExecution]
                         )
