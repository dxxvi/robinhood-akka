package home

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import akka.actor.{Actor, ActorRef, Props, Timers}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.logging.log4j.scala.Logging

import language.postfixOps
import scala.concurrent.duration._
import scala.concurrent.Future
import scala.sys.SystemProperties
import scala.util.{Failure, Success}

import spray.json._

object HttpActor {
    def props(username: String, password: String, wantedSymbols: String, robinhoodActor: ActorRef,
              enableTick: Boolean = true): Props =
        Props(new HttpActor(username, password, wantedSymbols, robinhoodActor, enableTick))

    final case class Credential(username: String, password: String)
    object CredentialProtocol extends DefaultJsonProtocol {
        implicit val credentialFormat: RootJsonFormat[Credential] = jsonFormat2(Credential)
    }

    final case object GetCurrentQuotes
    final case class GetTodayQuotes(wantedSymbols: String)
    final case class GetLastWeekQuotes(wantedSymbols: String)

    // latestTime is in the format YYYY-MM-dd'T'HH:mm:ss
    final case class GetOrders(url: Option[String] = None, latestTime: Option[String] = None)
    final case object GetUpdatedOrders

    final case object GetPositions
    final case object GetPortfolios

    final case object GetLoginToken
    final case class AccountUrl(accountUrl: String)

    final case class MakeOrder()
}

class HttpActor(username: String, password: String, wantedSymbols: String, robinhoodActor: ActorRef,
                enableTick: Boolean = true) extends Actor with SprayJsonSupport with Timers with Logging {
    import context.dispatcher

    import akka.http.scaladsl._
    import akka.http.scaladsl.marshalling._
    import akka.http.scaladsl.model._
    import akka.http.scaladsl.model.HttpMethods._
    import akka.http.scaladsl.model.headers._
    import akka.http.scaladsl.unmarshalling.Unmarshal
    import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
    import akka.http.scaladsl.settings.ConnectionPoolSettings
    import java.net.InetSocketAddress

    import HttpActor._

    val ROBINHOOD_SERVER = "https://api.robinhood.com"

    implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
    val http = Http(context.system)

    val systemProperties = new SystemProperties
    val proxyHost: Option[String] = systemProperties.get("https.proxyHost")
    val proxyPort: Option[String] = systemProperties.get("https.proxyPort")
    val connectionSettings: Option[ConnectionPoolSettings] = for {
        ph <- proxyHost
        pp <- proxyPort map (_.toInt)
    } yield ConnectionPoolSettings(context.system).withTransport(ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(ph, pp)))

    var senderActor: Option[ActorRef] = None

    var accountUrl: Option[String] = None
    var loginToken: Option[String] = None
    var gettingLoginToken = false

    if (enableTick) timers.startPeriodicTimer(0, Tick, 4019 milliseconds)

    override def receive: Receive = {
        case GetLoginToken =>
//            logger.debug("got GetLoginToken")
            if (loginToken.isEmpty && !gettingLoginToken) {
                logger.debug("Gonna get login token from robinhood")
                import CredentialProtocol._
                gettingLoginToken = true
                Marshal(Credential(username, password)).to[RequestEntity]
                        .flatMap(entity =>
                            sendSingleRequest(
                                HttpRequest(
                                    method = POST,
                                    uri = s"$ROBINHOOD_SERVER/do/",
                                    headers = List(Accept(MediaTypes.`application/json`)),
                                    entity = entity
                                ),
                                connectionSettings
                            )
                        )
                        .onComplete {
                            case Failure(ex) =>
                                logger.error(s"Error when accessing url to get token ${ex.getMessage}")
                                gettingLoginToken = false
                            case Success(hr) => hr.status match {
                                case StatusCodes.OK => Unmarshal(hr.entity).to[LoginToken] onComplete {
                                    case Failure(e) =>
                                        logger.error(s"Error in mapping HttpResponse entity to LoginToken: ${e.getMessage}")
                                        gettingLoginToken = false
                                    case Success(lt) => self ! lt.token
                                }
                                case x =>
                                    logger.error(s"Error: got response for getting token $x")
                                    hr.discardEntityBytes()
                                    gettingLoginToken = false
                            }
                        }
            }
        case x: String =>
            logger.debug(s"Login Token is $x")
            loginToken = Some(x)
            gettingLoginToken = false
        case GetCurrentQuotes =>
            val url = s"$ROBINHOOD_SERVER/quotes/?symbols=$wantedSymbols"
            val httpRequest = HttpRequest(GET, url) withHeaders Accept(MediaTypes.`application/json`)
            sendSingleRequest(httpRequest, connectionSettings) onComplete {
                case Failure(ex) => logger.error(s"Error when accessing $url: ${ex.getMessage}")
                case Success(hr) => hr.status match {
                    case StatusCodes.OK => Unmarshal(hr.entity).to[RobinhoodQuoteResults] onComplete {
                        case Failure(e) =>
                            logger.error(s"Error in mapping HttpResponse entity to RobinhoodQuoteResults: ${e.getMessage}")
                        case Success(rqrs) => robinhoodActor ! rqrs.results
                    }
                    case x =>
                        logger.error(s"Error when accessing $url, status code: $x")
                        hr.discardEntityBytes()
                }
            }
        case GetOrders(urlOption, latestTime) => loginToken match {
            case None =>
                self ! GetLoginToken
                self ! GetOrders(urlOption, latestTime)
            case Some(lt) =>
                val url = urlOption.getOrElse(s"$ROBINHOOD_SERVER/orders/")
//                logger.debug(s"Get orders from (${url.getClass}) $url")
                val ordersRequest = HttpRequest(GET, Uri(url))
                        .withHeaders(Accept(MediaTypes.`application/json`), RawHeader("Authorization", "Token " + lt))
                sendSingleRequest(ordersRequest, connectionSettings) onComplete {
                    case Failure(ex) => logger.error(s"Error when accessing $url: ${ex.getMessage}")
                    case Success(hr) => hr.status match {
                        case StatusCodes.OK => Unmarshal(hr.entity).to[RobinhoodOrderResults] onComplete {
                            case Failure(e) =>
                                logger.error(s"Error in mapping HttpResponse entity to RobinhoodOrderResults: ${e.getMessage}")
                            case Success(rors) =>
                                robinhoodActor ! rors.results
                                if (rors.next.isDefined && !rors.results.isEmpty) {
                                    latestTime.map(LocalDateTime.parse(_, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                                            .filter(_.isBefore(rors.results.last.createdAt))
                                            .foreach(_ => self ! GetOrders(rors.next, latestTime))
                                }
                        }
                        case x =>
                            logger.error(s"Error when accessing $url, status code: $x")
                            hr.discardEntityBytes()
                    }
                }
        }
        case GetUpdatedOrders =>
            val ua = LocalDateTime.now.minusHours(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + ".123456Z"
            self ! GetOrders(Some(s"$ROBINHOOD_SERVER/orders/?updated_at[gte]=$ua"))
        case Tick =>
            val now = LocalDateTime.now
            val openTime = now.withHour(9).withMinute(0).withSecond(9)
            val closeTime = now.withHour(17).withMinute(59).withSecond(59)
            if (now.isAfter(openTime) && now.isBefore(closeTime)) {
                self ! GetUpdatedOrders
                self ! GetCurrentQuotes
                self ! GetPositions
                self ! GetPortfolios
            }
        case GetPositions => loginToken match {
            case None =>
                self ! GetLoginToken
                self ! GetPositions
            case Some(lt) =>
                val url = s"$ROBINHOOD_SERVER/positions/"
                val ordersRequest = HttpRequest(GET, url)
                        .withHeaders(Accept(MediaTypes.`application/json`), RawHeader("Authorization", "Token " + lt))
                sendSingleRequest(ordersRequest, connectionSettings) onComplete {
                    case Failure(ex) => logger.error(s"Error when accessing $url: ${ex.getMessage}")
                    case Success(hr) => hr.status match {
                        case StatusCodes.OK => Unmarshal(hr.entity).to[RobinhoodPositionResults] onComplete {
                            case Failure(e) =>
                                logger.error(s"Error in mapping HttpResponse entity to RobinhoodPositionResults: ${e.getMessage}")
                            case Success(rprs) => robinhoodActor ! rprs.results
                        }
                        case x =>
                            logger.error(s"Error when accessing $url, status code: $x")
                            hr.discardEntityBytes()
                    }
                }
        }
        case GetPortfolios => loginToken match {
            case None =>
                self ! GetLoginToken
                self ! GetPortfolios
            case Some(lt) =>
                val url = s"$ROBINHOOD_SERVER/portfolios/"
                val ordersRequest = HttpRequest(GET, url)
                        .withHeaders(Accept(MediaTypes.`application/json`), RawHeader("Authorization", "Token " + lt))
                sendSingleRequest(ordersRequest, connectionSettings) onComplete {
                    case Failure(ex) => logger.error(s"Error when accessing $url: ${ex.getMessage}")
                    case Success(hr) => hr.status match {
                        case StatusCodes.OK => Unmarshal(hr.entity).to[RobinhoodPortfolioResults] onComplete {
                            case Failure(e) =>
                                logger.error(s"Error in mapping HttpResponse entity to RobinhoodPositionResults: ${e.getMessage}")
                            case Success(rprs) =>
                                import RobinhoodPortfolio._
                                rprs.results foreach { rp =>
                                    senderActor foreach { _ ! s"PORTFOLIO: ${rp.toJson.compactPrint}" }
                                }
                        }
                        case x =>
                            logger.error(s"Error when accessing $url, status code: $x")
                            hr.discardEntityBytes()
                    }
                }
        }
        case AccountUrl(s) => accountUrl = Some(s)
        case sa: ActorRef => senderActor = Some(sa)
        case x => logger.error(s"Unexpected message $x")
    }

    private def sendSingleRequest(httpRequest: HttpRequest,
                                  connectionSettings: Option[ConnectionPoolSettings]): Future[HttpResponse] =
        if (connectionSettings.nonEmpty)
            http.singleRequest(httpRequest, settings = connectionSettings.get)
        else
            http.singleRequest(httpRequest)
}
