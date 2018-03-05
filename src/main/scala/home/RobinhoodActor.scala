package home

import language.postfixOps
import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import org.apache.logging.log4j.scala.Logging

object RobinhoodActor {
    def props(username: String, password: String, wantedSymbols: String, enableTick: Boolean = true): Props =
        Props(new RobinhoodActor(username, password, wantedSymbols, enableTick))

    // latestTime is in the format YYYY-MM-dd'T'HH:mm:ss
    final case class GetOrdersForSymbol(symbol: String, latestTime: Option[String] = None)
}

class RobinhoodActor(username: String, password: String, wantedSymbols: String, enableTick: Boolean = true)
        extends Actor with Utils with Logging {
    import RobinhoodActor._
    var symbol2ActorRef = Map.empty[String, ActorRef]
    var instrument2ActorRef = Map.empty[String, ActorRef]
    var symbol2Instrument = Map.empty[String, String]
    var senderActor: Option[ActorRef] = None

    wantedSymbols.split(',').foreach { symbol =>
        symbol2ActorRef = symbol2ActorRef + (symbol -> context.actorOf(StockActor.props(symbol), s"$symbol-Actor"))
    }

    val httpActor: ActorRef =
        context.actorOf(HttpActor.props(username, password, wantedSymbols, self, enableTick), "httpActor")

    override def receive: Receive = {
        case robinhoodQuotes: Array[RobinhoodQuote] =>
//            logger.debug(s"Got ${robinhoodQuotes.length} quotes")
            robinhoodQuotes.foreach(rq => {
                symbol2Instrument = symbol2Instrument + (rq.symbol -> rq.instrument)
                symbol2ActorRef.get(rq.symbol) foreach {
                    stockActor => {
                        instrument2ActorRef = instrument2ActorRef + (rq.instrument -> stockActor)
                        stockActor ! robinhoodQuote2Quote(rq)
                    }
                }
            })
        case sa: ActorRef =>
            senderActor = Some(sa)
            symbol2ActorRef.foreach(_._2 ! sa)
            httpActor ! sa
        case robinhoodOrders: Array[RobinhoodOrder] => robinhoodOrders foreach { ro =>
            instrument2ActorRef.get(ro.instrument) foreach { _ ! robinhoodOrder2Order(ro) }
        }
        case robinhoodPositions: Array[RobinhoodPosition] => robinhoodPositions foreach { rp =>
            instrument2ActorRef.get(rp.instrument) foreach { _ ! robinhoodPosition2Position(rp) }
        }
        case GetOrdersForSymbol(symbol, latestTime) => symbol2Instrument.get(symbol) foreach { instrument =>
            httpActor ! HttpActor.GetOrders(
                Some(Uri("https://api.robinhood.com/orders/") withQuery Query(("instrument", instrument)) toString),
                latestTime
            )
        }
        case WriteOrdersToFile(symbol) =>
            println(s"symbol: $symbol symbol2ActorRef: $symbol2ActorRef")
            symbol2ActorRef.get(symbol).foreach(_ ! WriteOrdersToFile("anything"))
        case Tick => symbol2ActorRef.values.foreach(_ ! Tick)
        // the following is used in tests only. In real life, there's a timer that triggers HttpActor.
        case HttpActor.GetCurrentQuotes => httpActor ! HttpActor.GetCurrentQuotes
    }
}
