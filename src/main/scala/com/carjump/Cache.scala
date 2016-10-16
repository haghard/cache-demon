package com.carjump

import java.time._

import akka.http.scaladsl.Http
import com.carjump.compression._
import com.carjump.http.{ CacheResponseBody, ReqParams }
import com.rklaehn.radixtree.RadixTree
import scala.collection.immutable.SortedMap
import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import akka.actor.{ ActorLogging, Props, Actor }
import akka.stream.{ Supervision, ActorMaterializerSettings }

/**
 * This actor is responsible for catching exceptions inside Http flow
 */
object Cache {
  case class CacheException(message: String, cause: Throwable) extends Exception(message, cause)

  //Req
  case class FindInDecompressed(override val url: String, id: Int) extends ReqParams
  case class FindInIndex(override val url: String, id: Int) extends ReqParams

  //Resp
  case class CacheItem[T](value: Option[T] = None, lastUpdated: ZonedDateTime,
                          latency: Long = 0l, error: Option[String] = None) extends CacheResponseBody

  case class IndexEntry[T](endPosition: Int, line: T)

  implicit val ord = new Ordering[IndexEntry[String]] {
    override def compare(x: IndexEntry[String], y: IndexEntry[String]): Int = x.endPosition - y.endPosition
  }

  val nanoDivider = 1000
  val notFound = "empty"
  val notFoundEntry = IndexEntry(-1, "")

  import pprint._
  def pretty[T: PPrint](x: T): String = {
    import Config.Defaults._
    tokenize(x).mkString
  }

  def props(url: String, pref: String, pullInterval: FiniteDuration) =
    Props(new Cache(url, pref, pullInterval)).withDispatcher(Application.Dispatcher)
}

class Cache(override val url: String, override val pref: String, override val pullInterval: FiniteDuration) extends Actor
    with ActorLogging with Fetcher {
  import Cache._

  val clientFlowDecider: Supervision.Decider =
    (ex: Throwable) ⇒ {
      log.error(ex, "Http client flow error") //we don't want to lose state
      //self ! akka.actor.Status.Failure(ex) //if you want to crash this actor and lose internal state
      akka.stream.Supervision.Resume
    }

  implicit val timeout = akka.util.Timeout(3 second)
  implicit val mat = akka.stream.ActorMaterializer(
    ActorMaterializerSettings
      .create(system = context.system)
      .withInputBuffer(1, 1)
      .withSupervisionStrategy(clientFlowDecider)
      .withDispatcher("akka.fetch-dispatcher"))

  override val clientFlow = Http(context.system).outgoingConnection(url)

  override def preStart = start(self)
  override def postStop = log.info("Cache has been stopped")

  private def outOffRange(index: Int, maxIndex: Int): Boolean = index < 0 || index > maxIndex

  def updateState(state: Seq[Compressed[String]],
                  inMemoryIndex: Vector[IndexEntry[String]],
                  maxIndex: Int, mbSize: Float, lastUpdate: ZonedDateTime): Receive = {
    case FindInIndex(url, index0) ⇒
      if (!outOffRange(index0, maxIndex)) {
        val start = System.nanoTime
        //binary search is used since we have Vector
        import scala.collection.Searching._

        val searchResult: SearchResult = inMemoryIndex.search(IndexEntry(index0, ""))

        val item = inMemoryIndex.applyOrElse(searchResult.insertionPoint, { x: Int ⇒ notFoundEntry })
        if (item == notFoundEntry)
          sender() ! CacheItem[String](Option(s"Something went wrong for $index0. The index range:[0..$maxIndex]"), lastUpdated = lastUpdate)
        else
          sender() ! CacheItem[String](value = Some(item.line), latency = (System.nanoTime - start) / nanoDivider, lastUpdated = lastUpdate)
      } else {
        sender() ! CacheItem[String](error = Option(s"You asked for $index0. The index range:[0..$maxIndex]"), lastUpdated = lastUpdate)
      }

    //a very naive way to search. Just decompress on each request
    case FindInDecompressed(url, index0) ⇒
      if (!outOffRange(index0, maxIndex)) {
        val start = System.nanoTime
        val cache = Decompressor(state)
        val line = cache.applyOrElse(index0, { x: Int ⇒ notFound })
        if (line == notFound)
          sender() ! CacheItem[String](Option(s"Something went wrong for $index0. The index range:[0..$maxIndex]"), lastUpdated = lastUpdate)
        else
          sender() ! CacheItem[String](value = Some(line), latency = (System.nanoTime - start) / nanoDivider, lastUpdated = lastUpdate)
      } else {
        sender() ! CacheItem[String](error = Option(s"You asked for $index0. The index range:[0..$maxIndex]"), lastUpdated = lastUpdate)
      }

    case rawCache: Seq[String] @unchecked ⇒
      val cache = Compressor(rawCache)
      val size = org.openjdk.jol.info.GraphLayout.parseInstance(rawCache).totalSize().toFloat

      val index = cache.foldLeft((Vector[IndexEntry[String]](), -1)) { (acc, c) ⇒
        c match {
          case Single(ch) ⇒
            val offset = acc._2 + 1
            val item = IndexEntry(offset, ch)
            (acc._1.:+(item), offset)

          case Repeat(count, ch) ⇒
            val offset = acc._2 + count
            val item = IndexEntry(offset, ch)
            (acc._1.:+(item), offset)
        }
      }

      val mbSize = size / mbDivider

      log.info("Cache has been updated. The number of elements {} \nHeap size: {} mb \n{} \n{}",
        rawCache.size, mbSize, pretty(cache), pretty(index._1))

      (context become updateState(cache, index._1, rawCache.size - 1, mbSize, ZonedDateTime.now))

    case m: String if (m == onCompleteMessage) ⇒
      throw CacheException("Cache error",
        new Exception("Http client flow has been completed unexpectedly"))

    //case akka.actor.Status.Failure(ex) ⇒ throw CacheException(message = "Fetcher error", cause = ex)
  }

  val mbDivider = (1024 * 1024).toFloat

  override def receive = updateState(Seq[Compressed[String]](), Vector[IndexEntry[String]](),
    0, 0f, ZonedDateTime.now)
}