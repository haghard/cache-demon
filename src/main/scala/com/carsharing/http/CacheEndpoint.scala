package com.carsharing.http

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpHeader, HttpResponse }
import cats.data.Xor
import com.carsharing.AskSupport
import com.carsharing.Cache.{ CacheItem, FindInDecompressed }

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }

class CacheEndpoint(fetcher: ActorRef,
                    override val httpPath: String = "cache",
                    override implicit val askTimeout: akka.util.Timeout)(implicit system: ActorSystem, ec: ExecutionContext) extends AskSupport with HttpSupport {

  override val route = (get & path(httpPath / Segment)) { id ⇒
    withUri { url ⇒
      system.log.info(s"HTTP GET [$url]")
      get(complete(query(id, url)))
    }
  }

  private def query(id: String, url: String): Future[HttpResponse] = {
    Xor.catchNonFatal(id.toInt).fold(ex ⇒ Future.successful(internalError(s"Index should be a number. ${ex.getMessage}")), { idInt ⇒
      queryCache[CacheItem[String]](FindInDecompressed(url, idInt), fetcher).map {
        case Xor.Right(res) ⇒
          val headers = immutable.Seq[HttpHeader](RawHeader(latencyHeader, res.latency.toString), RawHeader(timeHeader, formatter.format(res.lastUpdated)))
          success(res.value.get, headers)
        case Xor.Left(ex) ⇒ internalError(ex)
      }
    })
  }
}