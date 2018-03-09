package home.robinhood

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import spray.json._

import scala.util.Try

object RobinhoodOrderExecution extends DefaultJsonProtocol {
    implicit object RobinhoodOrderExecutionJsonFormat extends RootJsonFormat[RobinhoodOrderExecution] with Utils {
        override def read(json: JsValue): RobinhoodOrderExecution =
            json.asJsObject("Unable to call asJsObject on RobinhoodOrderExecution jsValue").getFields(
                "timestamp", "price", "settlement_date", "id", "quantity"
            ) match {
                case Seq(
                    JsString(timestampe),
                    JsString(price),
                    JsString(settlementDate),
                    JsString(id),
                    JsString(quantity)
                ) => new RobinhoodOrderExecution(
                    Try(utcString2LocalDateTime(timestampe.replaceAll("\\.\\d{0,6}Z$", "Z"))).getOrElse(LocalDateTime.MIN),
                    Try(price.toDouble).getOrElse(Double.NaN),
                    Try(LocalDate.parse(settlementDate, DateTimeFormatter.ISO_LOCAL_DATE)).getOrElse(LocalDate.MIN),
                    id,
                    Try(quantity.toDouble.toInt).getOrElse(Int.MinValue)
                )
                case _ => throw DeserializationException(
                    s"Unable to deserialize ${json.compactPrint} 4 RobinhoodOrderExecution")
            }

        override def write(roe: RobinhoodOrderExecution): JsValue =
            serializationError("No need to serialize RobinhoodOrderExecution?")
    }
}

case class RobinhoodOrderExecution(timestamp: LocalDateTime, price: Double, settlementDate: LocalDate, id: String,
                                   quantity: Int)
