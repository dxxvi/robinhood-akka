package home.robinhood

import spray.json._

object LoginToken extends DefaultJsonProtocol {
    implicit object LoginTokenJsonFormat extends RootJsonFormat[LoginToken] {
        override def write(obj: LoginToken): JsValue = serializationError("No need to serialize LoginToken")

        override def read(json: JsValue): LoginToken =
            json.asJsObject("Unable to call asJsObject on LoginToken jsValue").getFields("token") match {
                case Seq(JsString(s)) => new LoginToken(s)
                case _ => throw DeserializationException(s"Unable to deserialize ${json.compactPrint} 4 LoginToken")
            }
    }
}

case class LoginToken(token: String)
