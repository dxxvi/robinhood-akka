package home.robinhood

import spray.json._

object RobinhoodPositionResults extends DefaultJsonProtocol {
    implicit object RobinhoodPositionResultsJsonFormat extends RootJsonFormat[RobinhoodPositionResults] {
        override def read(json: JsValue): RobinhoodPositionResults = {
            val fields = json.asJsObject("Unable to call asJsObject on RobinhoodPositionResults jsValue").fields
            val previous: Option[String] = fields.get("previous") collect {
                case x: JsString => x.value
            }
            val results = fields.get("results").map(_.convertTo[Array[RobinhoodPosition]])
                    .getOrElse(Array.empty[RobinhoodPosition])
            val next: Option[String] = fields.get("next") collect {
                case x: JsString => x.value
            }
            RobinhoodPositionResults(previous, results, next)
        }

        override def write(obj: RobinhoodPositionResults): JsValue =
            serializationError("No need to serialize RobinhoodPositionResults")
    }
}

case class RobinhoodPositionResults(previous: Option[String], results: Array[RobinhoodPosition], next: Option[String])