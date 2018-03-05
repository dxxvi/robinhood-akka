package home

import akka.actor.ActorRef
import org.apache.logging.log4j.scala.Logging
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.{OnWebSocketClose, OnWebSocketConnect, OnWebSocketMessage, WebSocket}

@WebSocket
class RobinhoodWebSocketImpl(robinhoodActor: Option[ActorRef]) extends RobinhoodWebSocket with Logging {
    private var session: Option[Session] = None

    @OnWebSocketConnect
    def connected(session: Session): Unit = {
        logger.debug("websocket connected")
        this.session = Some(session)
    }

    @OnWebSocketClose
    def closed(session: Session, statusCode: Int, reason: String): Unit = {
        logger.debug(s"websocket session closes because of $reason")
        this.session = None
    }

    @OnWebSocketMessage
    def onMessage(session: Session, message: String): Unit = logger.debug(s"Receive from websocket $message")

    override def send(message: String): Unit =
        session.foreach(_.getRemote.sendString(message))
}
