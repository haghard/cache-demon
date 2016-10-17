package com.carjump

import akka.actor.{ Stash, Props, Actor, ActorLogging }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpResponse, HttpRequest }
import akka.http.scaladsl.model.StatusCodes._
import akka.stream.{ Supervision, ActorMaterializerSettings }
import akka.stream.scaladsl.{ Framing, Sink }
import akka.util.ByteString
import com.carjump.http.ReqParams
import com.rklaehn.radixtree.RadixTree
import scala.concurrent.Future
import akka.pattern.pipe
import com.rklaehn.radixtree._
import cats.implicits._

object EnglishWordsCache {
  type TreeIndex = RadixTree[String, Long]

  case class SearchByPrefix(override val url: String, prefix: String) extends ReqParams

  def props(url: String) =
    Props(new EnglishWordsCache(url)).withDispatcher(Application.Dispatcher)
}

class EnglishWordsCache(url: String) extends Actor with ActorLogging with Stash {
  import EnglishWordsCache._
  val sep = ByteString("\n")
  val framing = Framing.delimiter(sep, maximumFrameLength = 100, allowTruncation = true)

  val clientDecider: Supervision.Decider =
    (ex: Throwable) ⇒ {
      log.error(ex, "Http client flow error")
      self ! akka.actor.Status.Failure(ex)
      akka.stream.Supervision.Stop
    }

  implicit val mat = akka.stream.ActorMaterializer(
    ActorMaterializerSettings
      .create(system = context.system)
      .withInputBuffer(32, 64)
      .withSupervisionStrategy(clientDecider)
      .withDispatcher("akka.fetch-dispatcher"))

  override def preStart() = fetch

  import algebra.ring.AdditiveMonoid
  import algebra.instances.all._

  override def receive = indexing(AdditiveMonoid[RadixTree[String, Long]])

  private def fetch: Future[HttpResponse] = {
    implicit val ex = mat.executionContext
    Http(context.system).singleRequest(HttpRequest(uri = url)).pipeTo(self)
  }

  val mbDivider = (1024 * 1024).toFloat

  private def indexing(M: AdditiveMonoid[RadixTree[String, Long]]): Receive = {
    case HttpResponse(OK, headers, entity, _) ⇒
      implicit val ex = mat.executionContext
      val f: Future[TreeIndex] = (entity.dataBytes.via(framing).map { line ⇒ line.utf8String }
        .runWith(Sink.fold((0l, M.zero)) { (pair, word) ⇒
          val num = pair._1 + 1l
          if (num % 10000 == 0)
            log.info("progress:{}", num)
          (num, M.plus(pair._2, RadixTree(word -> num)))
        })).map(_._2)
      f.pipeTo(self)
    case resp @ HttpResponse(code, _, _, _) ⇒
      log.info(s"Request failed with code: $code")
      resp.discardEntityBytes()
      (context stop self)
    case tree: RadixTree[String, Long] @unchecked ⇒
      val mbSize = org.openjdk.jol.info.GraphLayout.parseInstance(tree).totalSize().toFloat / mbDivider
      log.info("Tree the number of elements {} {} mb", tree.count, mbSize)
      unstashAll()
      context become ready(tree)
    case akka.actor.Status.Failure(ex) ⇒
      log.error(ex, "EnglishWordsCache has got error")
      (context stop self)

    case _ ⇒ stash()
  }

  private def ready(tree: RadixTree[String, Long]): Receive = {
    case SearchByPrefix(url, pref) ⇒
      val results = tree.filterPrefix(pref).keys
      log.info("Result size: {}", results.size)
      log.info(results.mkString("\n"))
  }
}