package com.carjump

import akka.stream.Materializer
import akka.actor.{ ActorRef, Cancellable, Actor, ActorLogging }
import akka.http.scaladsl.Http.OutgoingConnection
import akka.stream.scaladsl.{ Flow, Framing, Sink, Source }
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.forkjoin.ThreadLocalRandom
import akka.http.scaladsl.model.StatusCodes._

trait Fetcher {
  mixin: Actor with ActorLogging ⇒

  val sep = ByteString("\n")
  val framing = Framing.delimiter(sep, maximumFrameLength = 100, allowTruncation = true)

  val onCompleteMessage = "complete"

  def url: String

  def pref: String

  def pullInterval: FiniteDuration

  def clientFlow: Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]]

  private def timedFetch(implicit mat: Materializer): Source[Seq[String], Cancellable] = {
    implicit val ex = mat.executionContext
    Source
      .tick(pullInterval, pullInterval, ())
      .scan(0l)((acc, _) ⇒ acc + 1l)
      .mapAsync(1) { num ⇒
        log.info(s"Try to update cache №:$num")
        //if (ThreadLocalRandom.current().nextDouble() > 0.7) throw new Exception(s"Host $url is unavailable")
        fetch(url)
      }
  }

  private def fetch(url: String)(implicit mat: Materializer): Future[Seq[String]] = {
    implicit val ex = mat.executionContext
    (Source.single(HttpRequest(uri = pref)) via clientFlow).runWith(Sink.head)
      .flatMap { res ⇒
        res.status match {
          case OK ⇒ res.entity.dataBytes.via(framing)
            .map { line ⇒ line.utf8String }
            .runWith(Sink.seq[String])
          case other ⇒
            Future.failed(new Exception(s"Http error: $other"))
        }
      }
  }

  protected def start(sinkActor: ActorRef)(implicit mat: Materializer, timeout: akka.util.Timeout): Unit = {
    implicit val ex = mat.executionContext
    timedFetch.to(Sink.actorRef(sinkActor, onCompleteMessage)).run()
  }

  /**
   *
   */
  protected def printToFile(f: java.io.File)(op: java.io.PrintWriter ⇒ Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }
}
