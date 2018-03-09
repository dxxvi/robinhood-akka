package home.robinhood

import java.text.SimpleDateFormat
import java.time.{LocalDateTime, ZoneId}
import java.util.TimeZone

import scala.annotation.tailrec

trait Utils {
    // s in this format 2017-12-05T20:08:32Z
    def utcString2LocalDateTime(s: String): LocalDateTime = {
        val utcFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        val date = utcFormat.parse(s.replaceAll("\\.\\d{1,6}Z$", "Z"))
        LocalDateTime.ofInstant(date.toInstant, ZoneId.of("America/New_York"))
    }

    def robinhoodQuote2Quote(rq: RobinhoodQuote): Quote = Quote(rq.updatedAt, rq.lastTradePrice)

    def robinhoodOrder2Order(ro: RobinhoodOrder): Order = Order(ro.id, ro.side, ro.state, ro.createdAt, ro.fees,
        ro.price, ro.quantity, ro.averagePrice, ro.cumulativeQuantity)

    def robinhoodPosition2Position(rp: RobinhoodPosition): Position = Position(rp.averageBuyPrice, rp.quantity,
        rp.heldForBuy, rp.heldForSell)

    def buildOptionMap(args: Array[String]): Map[String, String] = {
        buildOptionMap(args, Map.empty[String, String])
    }
    @tailrec
    private def buildOptionMap(args: Array[String], optionMap: Map[String, String]): Map[String, String] = {
        if (args.isEmpty) optionMap else {
            val key = args.head
            val remainingArgs = args.tail
            if (key.startsWith("--") && !remainingArgs.isEmpty)
                buildOptionMap(remainingArgs.tail, optionMap + (key.replaceAll("^--", "") -> remainingArgs.head))
            else
                buildOptionMap(remainingArgs, optionMap)
        }
    }

    implicit object LocalDateTimeOrdering extends Ordering[LocalDateTime] {
        override def compare(x: LocalDateTime, y: LocalDateTime): Int = x compareTo y
    }
}
