package home.robinhood

import spray.json._

object RobinhoodOrderResults extends DefaultJsonProtocol {
    implicit object RobinhoodOrderResultsJsonFormat extends RootJsonFormat[RobinhoodOrderResults] {
        override def read(json: JsValue): RobinhoodOrderResults = {
            val fields = json.asJsObject("Unable to call asJsObject on RobinhoodOrderResults jsValue").fields
            val previous: Option[String] = fields.get("previous") collect {
                case JsString(x) => x
            }
            val results = fields.get("results").map(_.convertTo[Array[RobinhoodOrder]])
                    .getOrElse(Array.empty[RobinhoodOrder])
            val next: Option[String] = fields.get("next") collect {
                case JsString(x) => x
            }
            RobinhoodOrderResults(previous, results, next)
        }

        override def write(obj: RobinhoodOrderResults): JsValue =
            serializationError("No need to serialize RobinhoodOrderResults")
    }
}

case class RobinhoodOrderResults(previous: Option[String], results: Array[RobinhoodOrder], next: Option[String])
