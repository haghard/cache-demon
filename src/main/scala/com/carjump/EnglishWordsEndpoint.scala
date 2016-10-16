package com.carjump

import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, StatusCodes, HttpResponse }
import akka.util.ByteString
import com.carjump.EnglishWordsCache.SearchByPrefix
import com.carjump.http.HttpSupport

import scala.concurrent.ExecutionContext

class EnglishWordsEndpoint(wordsIndex: ActorRef,
                           override val httpPath: String = "words",
                           override implicit val askTimeout: akka.util.Timeout)(implicit system: ActorSystem, ec: ExecutionContext) extends AskSupport with HttpSupport {
  import akka.pattern.ask

  override val route = (get & path(httpPath / Segment)) { pref ⇒
    withUri { url ⇒
      system.log.info(s"HTTP GET [$url]")
      complete {
        (wordsIndex ? SearchByPrefix(url, pref)).map { _ ⇒
          HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, ByteString("Ok")))
        }
      }
    }
  }

  /*private def query(id: String, url: String): Future[HttpResponse] = {
    Xor.catchNonFatal(id.toInt).fold(ex ⇒ Future.successful(internalError(s"Index should be a number. ${ex.getMessage}")), { idInt ⇒
      queryCache[CacheItem[String]](FindInDecompressed(url, idInt), fetcher).map {
        case Xor.Right(res) ⇒
          val headers = immutable.Seq[HttpHeader](RawHeader(latencyHeader, res.latency.toString), RawHeader(timeHeader, formatter.format(res.lastUpdated)))
          success(res.value.get, headers)
        case Xor.Left(ex) ⇒ internalError(ex)
      }
    })
  }*/

}
