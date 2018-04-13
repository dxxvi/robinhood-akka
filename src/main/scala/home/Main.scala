package home

import java.util.Base64

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import home.robinhood.Utils
import org.apache.logging.log4j.scala.Logging
import spark.Spark._

import scala.concurrent.ExecutionContextExecutor

object Main extends Logging with Utils {
    def main(args: Array[String]): Unit = {
        val optionMap = buildOptionMap(args)
        if (!optionMap.contains("username")) println("Needs option --username")
        else if (!optionMap.contains("password")) println("Needs option --password")
        else if (!optionMap.contains("wanted-symbols")) println("Needs option --wanted-symmbol")
        else {
/*
            val sas = startActorSystem(optionMap.getOrElse("username", ""), optionMap.getOrElse("password", ""),
                optionMap.getOrElse("wanted-symbols", ""))

            startTheWeb(Some(sas._1), Some(sas._3))
*/
            init(
                optionMap.getOrElse("username", ""),
                new String(Base64.getDecoder.decode(optionMap.getOrElse("password", "eW91cl9wYXNzd29yZA=="))),
                optionMap.getOrElse("wanted-symbols", "")
            )
            awaitInitialization()
        }
    }

/*
    def startActorSystem(username: String, password: String, wantedSymbols: String): (ActorSystem, ActorMaterializer, ActorRef) = {
        implicit val system: ActorSystem = ActorSystem("R")
        val robinhoodActor = system.actorOf(RobinhoodActor.props(username, password, wantedSymbols))
        Tuple3(system, ActorMaterializer(), robinhoodActor)
    }

    def startTheWeb(aso: Option[ActorSystem], robinhoodActor: Option[ActorRef]): Unit = {
        staticFiles.location("/public")

        val robinhoodWebSocket = new RobinhoodWebSocketImpl(robinhoodActor)
        webSocket("/websocket", robinhoodWebSocket)

        get("/hello", (_, _) => "Hello World!")
        get("/PoisonPill", (_, _) => {
            aso foreach { system => {
                implicit val executionContext: ExecutionContextExecutor = system.dispatcher
                system.terminate().onComplete(_ => stop())
            }}
            "Bye bye!"
        })
    }
*/

    def init(username: String, password: String, wantedSymbols: String): (ActorSystem, ActorMaterializer) = {
        implicit val system: ActorSystem = ActorSystem("R")
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        val robinhoodActor = system.actorOf(RobinhoodActor.props(username, password, wantedSymbols))
        val robinhoodWebSocket = new RobinhoodWebSocketImpl(Some(robinhoodActor))
        webSocket("/api/websocket", robinhoodWebSocket)
        val senderActor = system.actorOf(SenderActor.props(robinhoodWebSocket))
        robinhoodActor ! senderActor
        staticFiles.location("/public")
        get("/hello", (_, _) => "Hello World!")
        get("/PoisonPill", (_, _) => {
            implicit val executionContext: ExecutionContextExecutor = system.dispatcher
            system.terminate().onComplete(_ => stop())
            "Bye bye!"
        })
        (system, materializer)
    }
}