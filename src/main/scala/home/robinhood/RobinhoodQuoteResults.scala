package home.robinhood

import spray.json._

object RobinhoodQuoteResults extends DefaultJsonProtocol {
    implicit object RobinhoodQuoteResultsJsonFormat extends RootJsonFormat[RobinhoodQuoteResults] with Utils {
        override def read(json: JsValue): RobinhoodQuoteResults = {
            val fields: Map[String, JsValue] =
                json.asJsObject("Unable to call asJsObject on RobinhoodQuoteResults jsValue").fields
            val results = fields.get("results").map(_.convertTo[Array[RobinhoodQuote]])
                    .getOrElse(Array.empty[RobinhoodQuote])
            RobinhoodQuoteResults(results)
        }

        override def write(obj: RobinhoodQuoteResults): JsValue = serializationError("not supported")
    }
}

case class RobinhoodQuoteResults(results: Array[RobinhoodQuote])
