package home

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}
import java.util.Base64

import language.postfixOps
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
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

    test("spray json RobinhoodOrderResults") {
        import spray.json._
        import RobinhoodOrderResults._
        val jsonString =
            """
              |{
              |  "previous": null,
              |  "results": [
              |    {
              |      "updated_at": "2017-12-15T19:28:29.314421Z",
              |      "ref_id": "03c270ac-b0ef-4a98-a322-e1144e380b18",
              |      "time_in_force": "gfd",
              |      "fees": "0.04",
              |      "cancel": null,
              |      "id": "212083de-009d-40a4-a03f-278f277c0010",
              |      "cumulative_quantity": "120.00000",
              |      "stop_price": null,
              |      "reject_reason": null,
              |      "instrument": "https://api.robinhood.com/instruments/940fc3f5-1db5-4fed-b452-f3a2e4562b5f/",
              |      "state": "filled",
              |      "trigger": "immediate",
              |      "override_dtbp_checks": false,
              |      "type": "limit",
              |      "last_transaction_at": "2017-12-15T19:28:29.114000Z",
              |      "price": "10.30000000",
              |      "executions": [
              |        {
              |          "timestamp": "2017-12-15T19:28:29.114000Z",
              |          "price": "10.30000000",
              |          "settlement_date": "2017-12-19",
              |          "id": "20839edd-c1a1-42a7-af6c-cb63dc6e790e",
              |          "quantity": "120.00000"
              |        }
              |      ],
              |      "extended_hours": true,
              |      "account": "https://api.robinhood.com/accounts/5RY82436/",
              |      "url": "https://api.robinhood.com/orders/212083de-009d-40a4-a03f-278f277c0010/",
              |      "created_at": "2017-12-15T17:28:22.882977Z",
              |      "side": "sell",
              |      "override_day_trade_checks": false,
              |      "position": "https://api.robinhood.com/positions/5RY82436/940fc3f5-1db5-4fed-b452-f3a2e4562b5f/",
              |      "average_price": "10.30000000",
              |      "quantity": "120.00000"
              |    },
              |    {
              |      "updated_at": "2017-12-15T17:15:51.708482Z",
              |      "ref_id": "3d226d88-787f-4aac-9c8a-92823c771fb7",
              |      "time_in_force": "gfd",
              |      "fees": "0.02",
              |      "cancel": null,
              |      "id": "9b8b5a05-6a2c-4c7b-b171-a61c685c924c",
              |      "cumulative_quantity": "16.00000",
              |      "stop_price": null,
              |      "reject_reason": null,
              |      "instrument": "https://api.robinhood.com/instruments/940fc3f5-1db5-4fed-b452-f3a2e4562b5f/",
              |      "state": "filled",
              |      "trigger": "immediate",
              |      "override_dtbp_checks": false,
              |      "type": "limit",
              |      "last_transaction_at": "2017-12-15T17:15:51.560000Z",
              |      "price": "10.24000000",
              |      "executions": [
              |        {
              |          "timestamp": "2017-12-15T17:15:51.560000Z",
              |          "price": "10.24000000",
              |          "settlement_date": "2017-12-19",
              |          "id": "21525948-7fc5-4426-80e4-872e2d342330",
              |          "quantity": "16.00000"
              |        }
              |      ],
              |      "extended_hours": true,
              |      "account": "https://api.robinhood.com/accounts/5RY82436/",
              |      "url": "https://api.robinhood.com/orders/9b8b5a05-6a2c-4c7b-b171-a61c685c924c/",
              |      "created_at": "2017-12-15T14:32:14.304016Z",
              |      "side": "sell",
              |      "override_day_trade_checks": false,
              |      "position": "https://api.robinhood.com/positions/5RY82436/940fc3f5-1db5-4fed-b452-f3a2e4562b5f/",
              |      "average_price": "10.24000000",
              |      "quantity": "16.00000"
              |    },
              |    {
              |      "updated_at": "2017-12-01T14:52:26.827424Z",
              |      "ref_id": "70e731c6-f401-4ffa-b850-2ecc43fbcb1e",
              |      "time_in_force": "gtc",
              |      "fees": "0.00",
              |      "cancel": null,
              |      "id": "e3efcf7a-1b87-4c01-8596-9942f32e92b8",
              |      "cumulative_quantity": "0.00000",
              |      "stop_price": null,
              |      "reject_reason": null,
              |      "instrument": "https://api.robinhood.com/instruments/8e08c691-869f-482c-8bed-39d026215a85/",
              |      "state": "cancelled",
              |      "trigger": "immediate",
              |      "override_dtbp_checks": false,
              |      "type": "limit",
              |      "last_transaction_at": "2017-12-01T14:52:26.675000Z",
              |      "price": "18.60000000",
              |      "executions": [],
              |      "extended_hours": true,
              |      "account": "https://api.robinhood.com/accounts/5RY82436/",
              |      "url": "https://api.robinhood.com/orders/e3efcf7a-1b87-4c01-8596-9942f32e92b8/",
              |      "created_at": "2017-11-30T22:17:17.245913Z",
              |      "side": "buy",
              |      "override_day_trade_checks": false,
              |      "position": "https://api.robinhood.com/positions/5RY82436/8e08c691-869f-482c-8bed-39d026215a85/",
              |      "average_price": null,
              |      "quantity": "1.00000"
              |    }
              |  ],
              |  "next": "https://api.robinhood.com/orders/?cursor=cD0yMDE3LTExLTMwKzIwJTNBNTIlM0ExNC4xMDE5MjclMkIwMCUzQTAw"
              |}
            """.stripMargin
        val robinhoodOrderResults = jsonString.parseJson.convertTo[RobinhoodOrderResults]
        robinhoodOrderResults.previous should be (None)
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
