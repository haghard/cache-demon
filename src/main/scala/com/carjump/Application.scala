package com.carjump

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializerSettings
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.server.RouteConcatenation._
import com.carjump.http.{ MetricsEndpoint, CacheEndpoint, IndexEndpoint }

import scala.util.{ Failure, Success }

object Application extends App {
  val cfg = ConfigFactory.load()
  val httpPort = cfg.getInt("http.port")
  implicit val system = ActorSystem("carjump", cfg)
  val Dispatcher = "akka.http-dispatcher"
  implicit val httpEc = system.dispatchers.lookup(Dispatcher)

  implicit val httpMat = akka.stream.ActorMaterializer(
    ActorMaterializerSettings
      .create(system = system)
      .withInputBuffer(32, 64)
      .withDispatcher(Dispatcher))

  val fetcher = system.actorOf(CacheGuardian.props, "guardian")

  val metrics = new MetricsEndpoint()
  val index = new IndexEndpoint(fetcher, askTimeout = akka.util.Timeout(cfg.getDuration("ask-cache.time-out")))
  val cache = new CacheEndpoint(fetcher, askTimeout = akka.util.Timeout(cfg.getDuration("ask-cache.time-out")))

  val wordsCache = system.actorOf(EnglishWordsCache.props(cfg.getString("words.url")))

  val words = new EnglishWordsEndpoint(wordsCache, askTimeout = akka.util.Timeout(cfg.getDuration("ask-cache.time-out")))

  Http().bindAndHandle(akka.http.scaladsl.server.RouteResult.route2HandlerFlow(cache.route ~ index.route ~ metrics.route ~ words.route),
    "0.0.0.0", httpPort)
    .onComplete {
      case Success(binding) ⇒
        val greeting = new StringBuilder()
          .append('\n')
          .append("=================================================================================================")
          .append('\n')
          .append(s"★ ★ ★ ★ ★ ★   CarJump http server localhost:$httpPort   ★ ★ ★ ★ ★ ★")
          .append('\n')
          .append(s"★ ★ ★ ★ ★ ★   Cache endpoint: 0.0.0.0:$httpPort/${cache.httpPath}/{id}    ★ ★ ★ ★ ★ ★")
          .append('\n')
          .append(s"★ ★ ★ ★ ★ ★   Index endpoint: 0.0.0.0:$httpPort/${index.httpPath}/{id}    ★ ★ ★ ★ ★ ★")
          .append('\n')
          .append(s"★ ★ ★ ★ ★ ★   Fetch Url: ${cfg.getString("ws.url")}${cfg.getString("ws.path")}    ★ ★ ★ ★ ★ ★")
          .append('\n')
          .append("=================================================================================================")
          .append('\n')
        system.log.info(greeting.toString)
      case Failure(ex) ⇒
        system.log.error(ex, s"Could'n start http server on $httpPort")
        sys.exit(-1)
    }
}
