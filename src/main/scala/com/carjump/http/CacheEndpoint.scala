package com.carjump.http

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpHeader, HttpResponse }
import akka.http.scaladsl.server._
import cats.data.Xor
import com.carjump.Cache.{ CacheItem, FindInDecompressed }
import com.carjump.AskSupport

import scala.concurrent.{ ExecutionContext, Future }
import scala.collection.immutable

class CacheEndpoint(fetcher: ActorRef,
                    override val httpPath: String = "cache",
                    override implicit val askTimeout: akka.util.Timeout)(implicit system: ActorSystem, ec: ExecutionContext) extends AskSupport with HttpSupport {

  override val route = cacheRoute

  private def cacheRoute(): Route =
    (get & path(httpPath / Segment)) { id ⇒
      withUri { url ⇒
        system.log.info(s"HTTP GET: [$url]")
        get(complete(query(id)))
      }
    }

  private def query(id: String): Future[HttpResponse] = {
    Xor.catchNonFatal(id.toInt).fold(ex ⇒ Future.successful(internalError(s"Index should be a number. ${ex.getMessage}")), { idInt ⇒
      queryCache[CacheItem[String]](FindInDecompressed(idInt), fetcher).map {
        case Xor.Right(res) ⇒
          val headers = immutable.Seq[HttpHeader](RawHeader(latencyHeader, res.latency.toString), RawHeader(timeHeader, formatter.format(res.lastUpdated)))
          success(res.value.get, headers)
        case Xor.Left(ex) ⇒ internalError(ex)
      }
    })
  }
}