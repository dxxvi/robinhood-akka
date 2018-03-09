package home.robinhood

import spray.json._

object RobinhoodPortfolioResults extends DefaultJsonProtocol {
    implicit object RobinhoodPortfolioResultsJsonFormat extends RootJsonFormat[RobinhoodPortfolioResults] {
        override def read(json: JsValue): RobinhoodPortfolioResults = {
            val fields = json.asJsObject("Unable to call asJsObject on RobinhoodPortfolioResults jsValue").fields
            val results = fields.get("results").map(_.convertTo[Array[RobinhoodPortfolio]])
                    .getOrElse(Array.empty[RobinhoodPortfolio])
            RobinhoodPortfolioResults(results)
        }

        override def write(obj: RobinhoodPortfolioResults): JsValue =
            serializationError("No need to serialize RobinhoodPortfolioResults")
    }
}

case class RobinhoodPortfolioResults(results: Array[RobinhoodPortfolio])
