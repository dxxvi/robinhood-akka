package home.robinhood

import spray.json._

import scala.util.Try

object RobinhoodPortfolio {
    implicit object RobinhoodPortfolioJsonFormat extends RootJsonFormat[RobinhoodPortfolio] {
        override def read(json: JsValue): RobinhoodPortfolio = {
            val fields = json.asJsObject("Unable to call asJsObject on RobinhoodPortfolio jsValue").fields
            val equity = fields.get("equity") collect {
                case JsString(s) => Try(s.toDouble) getOrElse Double.NaN
            } getOrElse Double.NaN
            val extendedHoursEquity = fields.get("extended_hours_equity") collect {
                case JsString(s) => Try(s.toDouble) getOrElse Double.NaN
            } getOrElse Double.NaN
            val marketValue = fields.get("market_value") collect {
                case JsString(s) => Try(s.toDouble) getOrElse Double.NaN
            } getOrElse Double.NaN
            RobinhoodPortfolio(equity, extendedHoursEquity, marketValue)
        }

        override def write(rp: RobinhoodPortfolio): JsValue = JsObject(
            "equity"              -> JsNumber(rp.equity),
            "extendedHoursEquity" -> JsNumber(rp.extendedHoursEquity),
            "marketValue"         -> JsNumber(rp.marketValue)
        )
    }
}

case class RobinhoodPortfolio(equity: Double, extendedHoursEquity: Double, marketValue: Double)
