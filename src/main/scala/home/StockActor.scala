package home

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZoneOffset}

import akka.actor.{Actor, ActorRef, Props, Timers}
import home.robinhood._
import org.apache.logging.log4j.scala.Logging

import language.postfixOps
import scala.concurrent.duration._
import scala.collection.SortedSet
import scala.sys.SystemProperties

object StockActor {
    def props(symbol: String): Props = Props(new StockActor(symbol))
}

class StockActor(symbol: String) extends Actor with Timers with Logging {
    private var quantity = 0
    private var heldForSell = 0
    private var quotes: Array[Quote] = Array.empty[Quote]

    private var autoBuy = false

    private var autoSell = false

    import Order.LocalDateTimeOrdering
    private var orders = SortedSet.empty[Order](Ordering[(LocalDateTime, String)].on(o => (o.createdAt, o.id)))

    var senderActor: Option[ActorRef] = None

    timers.startPeriodicTimer(0, Tick, 82419 milliseconds)
    private var quoteFileDate = LocalDate.MIN  // the date we already wrote quotes to file

    override def receive: Receive = {
        case q @ Quote(updatedAt, _) =>
                quotes = quotes match {
                    case Array() =>
                        sendToSenderActor(q)
                        Array(q)
                    case a: Array[Quote] if a.last.updatedAt != updatedAt =>
                        sendToSenderActor(q)
                        quotes :+ q
                    case _ => quotes
                }
            makeDecision()
        case o: Order =>
            orders = orders + o
            sendToSenderActor(o)
        case p: Position =>
            sendToSenderActor(p)
            quantity = p.quantity
            heldForSell = p.heldForSell
        case sa: ActorRef =>
            logger.debug(s"$symbol-Actor got the senderActor")
            senderActor = Some(sa)
        case Tick => new SystemProperties().get("java.io.tmpdir").foreach(tmpdir => {
            if (LocalDate.now.isAfter(quoteFileDate) &&
                    LocalDateTime.now.isAfter(LocalDateTime.now.withHour(16).withMinute(0))) {
                quoteFileDate = LocalDate.now
                Files.write(
                    Paths.get(tmpdir, s"$symbol-quotes-${LocalDate.now.format(DateTimeFormatter.ISO_LOCAL_DATE)}.txt"),
                    quotes
                            .map(q => {
                                val ua = q.updatedAt
                                s"${ua.getYear}-${ua.getMonthValue}-${ua.getDayOfMonth} " +
                                        s"${ua.getHour}:${ua.getMinute}:${ua.getSecond} ${q.lastTradePrice}"
                            })
                            .mkString("\n")
                            .getBytes,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
                )
                quotes = Array.empty[Quote]
                logger.debug(s"Wrote $symbol quotes to json file in $tmpdir")
            }
        })
        case WriteOrdersToFile(_) => new SystemProperties().get("java.io.tmpdir").foreach(tmpdir => {
            import spray.json._
            Files.write(
                Paths.get(tmpdir, s"$symbol-orders.json"),
                orders.toSeq.toJson.compactPrint.getBytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
            )
            logger.debug(s"$symbol-Actor gonna write ${orders.size} orders to json file in $tmpdir")
        })
        case x => logger.debug(s"Unknown message $x")
    }

    private def sendToSenderActor(q: Quote): Unit = {
        import spray.json._
        import Quote._
        val s = s"QUOTE: $symbol: " + q.toJson.compactPrint
        senderActor.foreach(_ ! s)
    }

    private def sendToSenderActor(o: Order): Unit = {
        import spray.json._
        import Order._
        val s = s"ORDER: $symbol: " + o.toJson.compactPrint
        logger.debug("who calls me?")
        senderActor.foreach(_ ! s)
    }

    private def sendToSenderActor(p: Position): Unit = {
        import spray.json._
        import Position._
        senderActor.foreach(_ ! s"POSITION: $symbol: " + p.toJson.compactPrint)
    }

    private def makeDecision(): Unit = {
        // TODO
    }
}
