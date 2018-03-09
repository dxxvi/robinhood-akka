package home.robinhood

import java.time.LocalDateTime
import language.postfixOps

import spray.json._

object Quote extends DefaultJsonProtocol {
    implicit object QuoteJsonFormat extends RootJsonFormat[Quote] {
        override def write(q: Quote): JsValue = JsObject(
            "year"   -> JsNumber(q.updatedAt.getYear),
            "month"  -> JsNumber(q.updatedAt.getMonthValue),
            "date"   -> JsNumber(q.updatedAt.getDayOfMonth),
            "hour"   -> JsNumber(q.updatedAt.getHour),
            "minute" -> JsNumber(q.updatedAt.getMinute),
            "second" -> JsNumber(q.updatedAt.getSecond),
            "quote"  -> JsNumber(q.lastTradePrice)
        )

        override def read(json: JsValue): Quote = {
            val fields = json.asJsObject("Unable to call asJsObject on Quote jsValue").fields
            val year = fields.get("year") collect { case JsNumber(x) => x.toInt } get
            val month = fields.get("month") collect { case JsNumber(x) => x.toInt } get
            val date = fields.get("date") collect { case JsNumber(x) => x.toInt } get
            val hour = fields.get("hour") collect { case JsNumber(x) => x.toInt } get
            val minute = fields.get("minute") collect { case JsNumber(x) => x.toInt } get
            val second = fields.get("second") collect { case JsNumber(x) => x.toInt } get
            val lastTradePrice = fields.get("quote").collect { case JsNumber(x) => x.toDouble }.get
            Quote(LocalDateTime.of(year, month, date, hour, minute, second), lastTradePrice)
        }
    }
}

case class Quote(updatedAt: LocalDateTime, lastTradePrice: Double)