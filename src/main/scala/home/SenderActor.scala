package home

import akka.actor.{Actor, Props}
import org.apache.logging.log4j.scala.Logging

object SenderActor {
    def props(webSocket: RobinhoodWebSocket): Props = Props(new SenderActor(webSocket))
}

class SenderActor(webSocket: RobinhoodWebSocket) extends Actor with Logging {
    override def receive: Receive = {
        case s: String =>
            webSocket.send(s)
            logger.debug(s"$sender sent $s to websocket")
        case x => logger.error(s"Got this message $x from $sender")
    }
}