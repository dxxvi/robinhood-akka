package home.robinhood

import java.time.LocalDateTime

import spray.json._

object Order extends DefaultJsonProtocol with Utils {
    implicit object OrderJsonFormat extends RootJsonFormat[Order] {
        override def write(o: Order): JsValue = JsObject(
            "id"                 -> JsString(o.id),
            "side"               -> JsString(o.side),
            "state"              -> JsString(o.state),
            "createdYear"        -> JsNumber(o.createdAt.getYear),
            "createdMonth"       -> JsNumber(o.createdAt.getMonthValue),
            "createdDay"         -> JsNumber(o.createdAt.getDayOfMonth),
            "createdHour"        -> JsNumber(o.createdAt.getHour),
            "createdMinute"      -> JsNumber(o.createdAt.getMinute),
            "createdSecond"      -> JsNumber(o.createdAt.getSecond),
            "fees"               -> (if (o.fees.isNaN) JsNumber(-1) else JsNumber(o.fees)),
            "price"              -> JsNumber(o.price),
            "quantity"           -> JsNumber(o.quantity),
            "averagePrice"       -> (if (o.averagePrice.isNaN) JsNumber(-1) else JsNumber(o.averagePrice)),
            "cumulativeQuantity" -> JsNumber(o.cumulativeQuantity)
        )

        override def read(json: JsValue): Order = deserializationError("No need to deserialize Order")
    }
}

case class Order(id: String, side: String, state: String, createdAt: LocalDateTime, fees: Double,
                 price: Double, quantity: Int,
                 averagePrice: Double, cumulativeQuantity: Int)
