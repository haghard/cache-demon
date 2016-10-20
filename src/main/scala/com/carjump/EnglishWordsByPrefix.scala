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

/**
 *
 * A very simple example where a RadixTree is very useful is word completion from a very large dictionary.
 * Filtering by prefix is extremely fast with a radix tree (worst case O(log(N)),
 * whereas it is worse than O(N) with SortedMap and HashMap.
 * Filtering by prefix will also benefit a lot from structural sharing.
 *
 */
object EnglishWordsByPrefix {
  type Index = RadixTree[String, Long]
  val mbDivider = (1024 * 1024).toFloat

  case class SearchByPrefix(override val url: String, prefix: String) extends ReqParams

  def props(url: String) =
    Props(new EnglishWordsByPrefix(url)).withDispatcher(Application.Dispatcher)
}

class EnglishWordsByPrefix(url: String) extends Actor with ActorLogging with Stash {
  import EnglishWordsByPrefix._
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

  private def indexing(M: AdditiveMonoid[RadixTree[String, Long]]): Receive = {
    case HttpResponse(OK, headers, entity, _) ⇒
      implicit val ex = mat.executionContext
      val f: Future[Index] = (entity.dataBytes.via(framing).map { line ⇒ line.utf8String }
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
      context become active(tree)
    case akka.actor.Status.Failure(ex) ⇒
      log.error(ex, "EnglishWordsCache has got error")
      (context stop self)

    case _ ⇒ stash()
  }

  private def active(tree: RadixTree[String, Long]): Receive = {
    case SearchByPrefix(url, pref) ⇒
      val results = tree.filterPrefix(pref).keys
      log.info("Result size: {}", results.size)
      log.info(results.mkString("\n"))
  }
}