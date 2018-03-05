package home

import java.net.URI
import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import org.apache.logging.log4j.scala.Logging
import org.eclipse.jetty.websocket.api.{Session, WebSocketAdapter}
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.eclipse.jetty.websocket.common.WebSocketSession
import org.scalatest.{FunSuite, Matchers}
import spark.Spark

import language.postfixOps
import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration._

class MainSpec extends FunSuite with Matchers with Logging {
    private val password = new String(Base64.getDecoder.decode("VGhAaTE5MDRIQG5n"))

    test("the web endpoints and socket should work") {
        val x = Main.init("dxxvi", password, "AMD,HTZ")
        Spark.awaitInitialization()

        implicit val system: ActorSystem = x._1
        implicit val materializer: ActorMaterializer = x._2
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher

        val http = Http(system)

        val httpResponse = Await.result(http.singleRequest(HttpRequest(HttpMethods.GET, "http://localhost:4567/hello")),
            5 seconds)
        val body = Await.result(httpResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _), 5 seconds)
        body.utf8String should be ("Hello World!")

        val webSocketClient = new WebSocketClient()
        try {
            webSocketClient.start()
            val session = webSocketClient.connect(new WebSocketAdapter() {
                override def onWebSocketConnect(sess: Session): Unit = {
                    logger.debug(s"websocket connect $sess")
                    super.onWebSocketConnect(sess)
                }

                override def onWebSocketClose(statusCode: Int, reason: String): Unit = {
                    logger.debug(s"websocket close: status: $statusCode, reason: $reason")
                    super.onWebSocketClose(statusCode, reason)
                }

                override def onWebSocketError(cause: Throwable): Unit = {
                    logger.debug(s"websocket error ${cause.getMessage}")
                    super.onWebSocketError(cause)
                }

                override def onWebSocketText(message: String): Unit = {
                    logger.debug(s"websocket text $message")
                    super.onWebSocketText(message)
                }
            }, URI.create("ws://localhost:4567/websocket")).get()

            Thread.sleep(82419)
            session.close()
        }
        finally {
            webSocketClient.stop()
        }

        Spark.stop()
        system.terminate()
    }

    test("Write orders to file") {
        val t = 19824
        implicit val system: ActorSystem = ActorSystem("R")
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        val robinhoodActor = system.actorOf(RobinhoodActor.props("dxxvi", password, "AMD,HTZ", false))
        robinhoodActor ! HttpActor.GetCurrentQuotes
        Thread.sleep(t)
        robinhoodActor ! RobinhoodActor.GetOrdersForSymbol("AMD", Some("2017-11-30T00:00:01"))
        Thread.sleep(t)
        robinhoodActor ! WriteOrdersToFile("AMD")
        Thread.sleep(t)
    }
}
