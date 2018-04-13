package home

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import java.util.Base64

import language.postfixOps
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import home.robinhood.{Quote, RobinhoodOrder, RobinhoodOrderResults, RobinhoodQuote, RobinhoodQuoteResults, Utils}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

import scala.io.Source

/*
 * To run a single test/method: mvn -Dsuites='home.BasicSpec @get current quotes' test
 */
class BasicSpec extends TestKit(ActorSystem("R")) with FunSuiteLike with Matchers with BeforeAndAfterAll
        with ImplicitSender {
    private val password = new String(Base64.getDecoder.decode("VGhAaTE5MDRIQG5n"))
    override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

    test("get login token") {
        val probe = TestProbe()
        val httpActor = system.actorOf(HttpActor.props("dxxvi", password, "AMD,HTZ", probe.ref, false))
        httpActor.tell(HttpActor.GetLoginToken, probe.ref)
        httpActor.tell(HttpActor.GetLoginToken, probe.ref)
        httpActor.tell(HttpActor.GetLoginToken, probe.ref)
        httpActor.tell(HttpActor.GetLoginToken, probe.ref)
        httpActor.tell(HttpActor.GetLoginToken, probe.ref)
        Thread.sleep(19482)
    }

    test("get current quotes") {
        val t = 8019
        val probe = TestProbe()
        val httpActor = system.actorOf(HttpActor.props("dxxvi", password, "AMD,HTZ", probe.ref, false))
        httpActor.tell(HttpActor.GetCurrentQuotes, probe.ref)
        Thread.sleep(t)
        val robinhoodQuotes = probe.expectMsgType[Array[RobinhoodQuote]](t milliseconds)
        robinhoodQuotes.map(_.symbol).toSet should be (Set("HTZ", "AMD"))
    }

    test("get latest orders") {
        val t = 19482
        val probe = TestProbe()
        val httpActor = system.actorOf(HttpActor.props("dxxvi", password, "AMD,HTZ", probe.ref, false))
        httpActor ! HttpActor.GetOrders()
        val robinhoodOrderArray = probe.expectMsgType[Array[RobinhoodOrder]](t milliseconds)
        robinhoodOrderArray.length should be (100)
        println(robinhoodOrderArray.mkString("\n"))
        Thread.sleep(t)
    }

    test("change farBackForOrders") {
        val t = 19482
        val probe = TestProbe()
        val httpActor = system.actorOf(HttpActor.props("dxxvi", password, "AMD,HTZ", probe.ref, false))
        httpActor ! HttpActor.GetOrders(None, Some("2017-11-30T00:00:00"))
        val robinhoodOrderArray = probe.expectMsgType[Array[RobinhoodOrder]](t milliseconds)
        robinhoodOrderArray.length should be (100)
        Thread.sleep(t)
    }

    test("get updated orders") {
        val t = 19482
        val probe = TestProbe()
        val httpActor = system.actorOf(HttpActor.props("dxxvi", password, "AMD,HTZ", probe.ref, false))
        httpActor ! HttpActor.GetUpdatedOrders
        Thread.sleep(t)
    }

    test("spray json RobinhoodQuote") {
        import spray.json._
        import RobinhoodQuote._

        var jsonString =
            """
              |{
              |  "ask_price":"10.0200",
              |  "ask_size":5900,
              |  "bid_price":"10.0100",
              |  "bid_size":300,
              |  "last_trade_price":"9.9100",
              |  "last_extended_hours_trade_price":"10.0300",
              |  "previous_close":"10.0300",
              |  "adjusted_previous_close":"10.0300",
              |  "previous_close_date":"2017-12-04",
              |  "symbol":"AMD",
              |  "trading_halted":false,
              |  "has_traded":true,
              |  "last_trade_price_source":"consolidated",
              |  "updated_at":"2017-12-05T22:55:34Z",
              |  "instrument":"https://api.robinhood.com/instruments/940fc3f5-1db5-4fed-b452-f3a2e4562b5f/"
              |}
            """.stripMargin
        var robinhoodQuote = jsonString.parseJson.convertTo[RobinhoodQuote]
        println(s"$robinhoodQuote")
        robinhoodQuote.symbol should be ("AMD")
        robinhoodQuote.updatedAt should be (LocalDateTime.of(2017, 12, 5, 17, 55, 34))

        jsonString =
            """
              |{
              |  "previous_close_date":"2017-12-08",
              |  "previous_close":"9.9400",
              |  "last_trade_price":"10.0200",
              |  "last_trade_price_source":"nls",
              |  "adjusted_previous_close":"9.9400",
              |  "symbol":"AMD",
              |  "ask_size":6200,
              |  "has_traded":true,
              |  "bid_price":"10.0500",
              |  "ask_price":"10.0600",
              |  "updated_at":"2017-12-11T14:56:48Z",
              |  "instrument":"https://api.robinhood.com/instruments/940fc3f5-1db5-4fed-b452-f3a2e4562b5f/",
              |  "last_extended_hours_trade_price":null,
              |  "bid_size":6400,
              |  "trading_halted":false}
            """.stripMargin
        robinhoodQuote = jsonString.parseJson.convertTo[RobinhoodQuote]
        println(s"$robinhoodQuote")
        robinhoodQuote.symbol should be ("AMD")
        robinhoodQuote.updatedAt should be (LocalDateTime.of(2017, 12, 11, 9, 56, 48))
    }

    test("spary json RobinhoodQuoteResults") {
        import spray.json._
        import RobinhoodQuoteResults._
        val jsonString =
            """
              |{
              |  "results":[
              |    {
              |      "ask_price":"10.0000",
              |      "ask_size":1500,
              |      "bid_price":"9.9900",
              |      "bid_size":100,
              |      "last_trade_price":"9.9100",
              |      "last_extended_hours_trade_price":"9.9800",
              |      "previous_close":"10.0300",
              |      "adjusted_previous_close":"10.0300",
              |      "previous_close_date":"2017-12-04",
              |      "symbol":"AMD",
              |      "trading_halted":false,
              |      "has_traded":true,
              |      "last_trade_price_source":"consolidated",
              |      "updated_at":"2017-12-06T00:32:10Z",
              |      "instrument":"https://api.robinhood.com/instruments/940fc3f5-1db5-4fed-b452-f3a2e4562b5f/"
              |    },
              |    {
              |      "ask_price":"19.5500",
              |      "ask_size":400,
              |      "bid_price":"19.2700",
              |      "bid_size":200,
              |      "last_trade_price":"19.2700",
              |      "last_extended_hours_trade_price":"19.2700",
              |      "previous_close":"19.9300",
              |      "adjusted_previous_close":"19.9300",
              |      "previous_close_date":"2017-12-04",
              |      "symbol":"HTZ",
              |      "trading_halted":false,
              |      "has_traded":true,
              |      "last_trade_price_source":"consolidated",
              |      "updated_at":"2017-12-05T23:01:51Z",
              |      "instrument":"https://api.robinhood.com/instruments/8e08c691-869f-482c-8bed-39d026215a85/"
              |    }
              |  ]
              |}
            """.stripMargin
        val robinhoodQuoteResults = jsonString.parseJson.convertTo[RobinhoodQuoteResults]
        robinhoodQuoteResults.results.length should be (2)
        robinhoodQuoteResults.results(0).symbol should be ("AMD")
    }

    test("spray json RobinhoodOrder") {
        import spray.json._
        import RobinhoodOrder._
        val jsonString =
            """
              |{
              |  "updated_at": "2017-12-15T19:28:29.314421Z",
              |  "ref_id": "03c270ac-b0ef-4a98-a322-e1144e380b18",
              |  "time_in_force": "gfd",
              |  "fees": "0.04",
              |  "cancel": null,
              |  "id": "212083de-009d-40a4-a03f-278f277c0010",
              |  "cumulative_quantity": "120.00000",
              |  "stop_price": null,
              |  "reject_reason": null,
              |  "instrument": "https://api.robinhood.com/instruments/940fc3f5-1db5-4fed-b452-f3a2e4562b5f/",
              |  "state": "filled",
              |  "trigger": "immediate",
              |  "override_dtbp_checks": false,
              |  "type": "limit",
              |  "last_transaction_at": "2017-12-15T19:28:29.114000Z",
              |  "price": "10.30000000",
              |  "executions": [
              |    {
              |      "timestamp": "2017-12-15T19:28:29.114000Z",
              |      "price": "10.30000000",
              |      "settlement_date": "2017-12-19",
              |      "id": "20839edd-c1a1-42a7-af6c-cb63dc6e790e",
              |      "quantity": "120.00000"
              |    }
              |  ],
              |  "extended_hours": true,
              |  "account": "https://api.robinhood.com/accounts/5RY82436/",
              |  "url": "https://api.robinhood.com/orders/212083de-009d-40a4-a03f-278f277c0010/",
              |  "created_at": "2017-12-15T17:28:22.882977Z",
              |  "side": "sell",
              |  "override_day_trade_checks": false,
              |  "position": "https://api.robinhood.com/positions/5RY82436/940fc3f5-1db5-4fed-b452-f3a2e4562b5f/",
              |  "average_price": "10.30000000",
              |  "quantity": "120.00000"
              |}
            """.stripMargin
        val robinhoodOrder = jsonString.parseJson.convertTo[RobinhoodOrder]
        robinhoodOrder.updatedAt should be (LocalDateTime.of(2017, 12, 15, 14, 28, 29))
    }

    test("spray json RobinhoodOrderResults, print them out") {
        import spray.json._
        import robinhood.RobinhoodOrderResults._
        val jsonString = Source.fromInputStream(classOf[BasicSpec].getResourceAsStream("/MYGN-orders.json")).mkString
        val robinhoodOrderResults = jsonString.parseJson.convertTo[RobinhoodOrderResults]
        robinhoodOrderResults.previous should be (None)

        class ROWrapper(var no: Option[Int] = None, val ro: RobinhoodOrder)

        import scala.util.control.Breaks._
        val a: Array[ROWrapper] = robinhoodOrderResults.results
                .collect {
                    case ro: RobinhoodOrder if ro.state == "filled" && ro.quantity == ro.cumulativeQuantity => new ROWrapper(None, ro)
                }
        var transactionNumber = 1
        var coupleFound = true
        while (coupleFound) {
            coupleFound = false
            val b = a.filter(_.no.isEmpty)
            for (i <- 0 to b.length if b(i).no.isEmpty) {
                val j = i + 1
                if (b(i).ro.quantity == b(j).ro.quantity) {
                    if (
                        (b(i).ro.side == "buy" && b(j).ro.side == "sell" && b(i).ro.price < b(j).ro.price) ||
                        (b(i).ro.side == "sell" && b(j).ro.side == "buy" && b(i).ro.price > b(j).ro.price)
                    ) {
                        b(i).no = Some(transactionNumber)
                        b(j).no = Some(transactionNumber)
                        transactionNumber += 1
                        coupleFound = true
                        break
                    }
                }
            }
        }
//                .foreach(ro => println(f"${ro.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)} ${ro.side.toUpperCase}%-4s ${ro.quantity}%3d x ${ro.price}%5.2f"))
    }

    test("regular expression") {
        val s = "2017-12-14T15:03:35.Z"
        println(s.replaceAll("\\.\\d{0,6}Z$", "Z"))
    }

    test("test Utils.utcString2LocalDateTime") {
        case class X(s: String) extends Utils {
            def toLocalDateTime: LocalDateTime = utcString2LocalDateTime(s)
        }

        X("2017-12-05T20:08:32Z").toLocalDateTime should be (LocalDateTime.of(2017, 12, 5, 15, 8, 32))
        X("2017-12-05T01:04:19Z").toLocalDateTime should be (LocalDateTime.of(2017, 12, 4, 20, 4, 19))
    }

    test("test Utils.buildOptionMap") {
        case class X(args: Array[String]) extends Utils {
            def buildOptionMap: Map[String, String] = buildOptionMap(args)
        }

        X(Array.empty[String]).buildOptionMap should be (Map.empty[String, String])
        X(Array("username")).buildOptionMap should be (Map.empty[String, String])
        X(Array("--username", "dxxvi", "--password", "abc123", "--wanted-symbols", "AMD,HTZ")).buildOptionMap should be
        Map("password" -> "abc123", "wanted-symbols" -> "AMD,HTZ", "username" -> "dxxvi")
    }

    test("read quote json file") {
        import spray.json._
        import Quote._
        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val line = Source.fromFile("/home/ly/Robinhood-2018-01-05/HTZ-quotes.json").mkString.parseJson
                .convertTo[Array[Quote]]
                .map(q => s"${q.updatedAt.format(dtf)} ${q.lastTradePrice}")
                .mkString("\n")
        Files.write(Paths.get("/tmp", "HTZ.txt"), line.getBytes,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }
}
