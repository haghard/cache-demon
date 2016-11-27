package com.carsharing

import cats.data.Xor
import akka.http.scaladsl.model._
import akka.actor.{ ActorRef, ActorSystem }
import akka.http.scaladsl.model.headers.RawHeader
import com.carsharing.EnglishWordsByPrefix.{ SearchByPrefix, SearchResult }
import com.carsharing.http.HttpSupport

import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }

class EnglishWordsEndpoint(wordsIndex: ActorRef, override val httpPath: String = "words",
                           override implicit val askTimeout: akka.util.Timeout)(implicit system: ActorSystem, ec: ExecutionContext) extends AskSupport with HttpSupport {

  override val route = path(httpPath / Segment) { pref ⇒
    get {
      withUri { url ⇒
        complete {
          query(url, pref)
        }
      }
    }
  }

  import spray.json._
  import DefaultJsonProtocol._
  private def query(url: String, pref: String): Future[HttpResponse] = {
    queryCache[SearchResult](SearchByPrefix(url, pref), wordsIndex).map {
      case Xor.Right(res) ⇒
        successJson(res.words.toJson.prettyPrint,
          immutable.Seq[HttpHeader](RawHeader(latencyHeader, res.latency.toString), RawHeader(timeHeader, formatter.format(res.lastUpdated))))
      case Xor.Left(ex) ⇒ internalError(ex)
    }
  }
}