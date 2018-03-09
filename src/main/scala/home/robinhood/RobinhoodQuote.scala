package home.robinhood

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import spray.json._

import scala.util.Try

object RobinhoodQuote extends DefaultJsonProtocol {
    implicit object RobinhoodQuoteJsonFormat extends RootJsonFormat[RobinhoodQuote] with Utils {
        override def read(json: JsValue): RobinhoodQuote = {
            json.asJsObject("Unable to call asJsObject on RobinhoodQuote jsValue.")
                    .getFields(
                        "ask_price",
                        "ask_size",
                        "bid_price",
                        "bid_size",
                        "last_trade_price",
                        //                        "last_extended_hours_trade_price",  // can be a "1.234" or null, so ignore it
                        "previous_close",
                        "adjusted_previous_close",
                        "previous_close_date",
                        "symbol",
                        "trading_halted",
                        "has_traded",
                        "last_trade_price_source",
                        "updated_at",
                        "instrument") match {
                case Seq(
                JsString(askPrice),
                JsNumber(askSize),
                JsString(bidPrice),
                JsNumber(bidSize),
                JsString(lastTradePrice),
                //                    JsString(lastExtendedHoursTradePrice),
                JsString(previousClose),
                JsString(adjustedPreviousClose),
                JsString(previousCloseDate),
                JsString(symbol),
                JsBoolean(tradingHalted),
                JsBoolean(hasTraded),
                JsString(lastTradePriceSource),
                JsString(updatedAt),
                JsString(instrument)
                ) =>
                    new RobinhoodQuote(
                        Try(askPrice.toDouble).getOrElse(Double.NaN),
                        askSize.toInt,
                        Try(bidPrice.toDouble).getOrElse(Double.NaN),
                        bidSize.toInt,
                        Try(lastTradePrice.toDouble).getOrElse(Double.NaN),
                        //                        Try(lastExtendedHoursTradePrice.toDouble).getOrElse(Double.NaN),
                        Try(previousClose.toDouble).getOrElse(Double.NaN),
                        Try(adjustedPreviousClose.toDouble).getOrElse(Double.NaN),
                        Try(LocalDate.parse(previousCloseDate, DateTimeFormatter.ISO_LOCAL_DATE)).getOrElse(LocalDate.MIN),
                        symbol,
                        tradingHalted,
                        hasTraded,
                        lastTradePriceSource,
                        Try(utcString2LocalDateTime(updatedAt)).getOrElse(LocalDateTime.MIN),
                        instrument
                    )
                case _ => throw DeserializationException(s"Unable to deserialize ${json.compactPrint} 4 RobinhoodQuote")
            }
        }
        override def write(rq: RobinhoodQuote): JsValue = JsObject(
            "askPrice"      -> JsNumber(rq.askPrice),
            "askSize"       -> JsNumber(rq.askSize),
            "symbol"        -> JsString(rq.symbol),
            "tradingHalted" -> JsTrue
        )
    }
}

case class RobinhoodQuote(askPrice: Double, askSize: Int, bidPrice: Double, bidSize: Int, lastTradePrice: Double,
                          /*lastExtendedHoursTradePrice: Double,*/ previousClose: Double, adjustedPreviousClose: Double,
                          previousCloseDate: LocalDate, symbol: String, tradingHalted: Boolean, hasTraded: Boolean,
                          lastTradePriceSource: String, updatedAt: LocalDateTime, instrument: String)
