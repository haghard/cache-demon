package com.carjump

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import com.typesafe.config.ConfigFactory
import akka.http.scaladsl.server.RouteConcatenation._
import com.carjump.http.{ CacheEndpoint, IndexEndpoint }

import scala.util.{ Failure, Success }

object Application extends App {
  val cfg = ConfigFactory.load()
  val httpPort = cfg.getInt("http.port")
  implicit val system = ActorSystem("carjump", cfg)
  implicit val httpEc = system.dispatchers.lookup("akka.http-dispatcher")
  implicit val httpMat = akka.stream.ActorMaterializer.create(system)

  val fetcher = system.actorOf(CacheGuardian.props, "guardian")

  val index = new IndexEndpoint(fetcher, akka.util.Timeout(cfg.getDuration("cache.time-out")))
  val cache = new CacheEndpoint(fetcher, akka.util.Timeout(cfg.getDuration("cache.time-out")))

  Http().bindAndHandle(akka.http.scaladsl.server.RouteResult.route2HandlerFlow(cache.router ~ index.router), "127.0.0.1", httpPort)
    .onComplete {
      case Success(binding) ⇒
        val greeting = new StringBuilder()
          .append('\n')
          .append("=================================================================================================")
          .append('\n')
          .append(s"★ ★ ★ ★ ★ ★   CarJump http server localhost:$httpPort   ★ ★ ★ ★ ★ ★")
          .append('\n')
          .append(s"★ ★ ★ ★ ★ ★   Cache endpoint: localhost:$httpPort/${cache.httpPath}/{id}    ★ ★ ★ ★ ★ ★")
          .append('\n')
          .append(s"★ ★ ★ ★ ★ ★   Index endpoint: localhost:$httpPort/${index.httpPath}/{id}    ★ ★ ★ ★ ★ ★")
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

  system.registerOnTermination {
    system.log.info("★ ★ ★ ★ ★ ★ CarJump server exit ★ ★ ★ ★ ★ ★")
  }
}