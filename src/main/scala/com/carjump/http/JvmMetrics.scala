package com.carjump.http

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.ByteString
import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisher
import java.time.format.DateTimeFormatter
import java.time.{ ZoneOffset, ZonedDateTime }
import akka.stream.actor.ActorPublisherMessage.{ SubscriptionTimeoutExceeded, Cancel, Request }

class JvmMetrics extends ActorPublisher[ByteString] with ActorLogging {
  val encoding = "UTF-8"
  val pushTimeout = 5 seconds
  val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  implicit val _ = context.system.dispatchers.lookup(MetricsEndpoint.Dispatcher)

  //com.twitter.jvm.CpuProfile.record(com.twitter.util.Duration(5, TimeUnit.SECONDS), 50)
  //.writeGoogleProfile()

  implicit val timeout = akka.util.Timeout(1 seconds)

  override def preStart = log.info("JvmMetrics has been started")

  override def receive: Receive = {
    case 'Write ⇒
      onNext(ByteString(formatter.format(ZonedDateTime.now(ZoneOffset.UTC)) + "\n", encoding))
      sender() ! true
    case req @ Request(n) ⇒
      log.info("JvmMetrics req {}", n)
      deliver(n)
    case SubscriptionTimeoutExceeded ⇒
      log.info("JvmMetrics subscription timeout")
      context.stop(self)
    case Cancel ⇒
      log.info("JvmMetrics canceled")
      context.stop(self)
  }

  private def deliver(n: Long): Unit = {
    if (isActive && totalDemand > 0 && n > 0) {
      akka.pattern.after(pushTimeout, context.system.scheduler)(self.ask('Write))
        .onComplete(_ ⇒ deliver(n - 1))
    }
  }
}