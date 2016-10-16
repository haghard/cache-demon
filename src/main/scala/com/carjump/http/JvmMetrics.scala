package com.carjump.http

import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.util.ByteString
import akka.actor.ActorLogging
import akka.stream.actor.ActorPublisher
import java.time.format.DateTimeFormatter
import akka.stream.actor.ActorPublisherMessage.{ SubscriptionTimeoutExceeded, Cancel, Request }

class JvmMetrics extends ActorPublisher[ByteString] with ActorLogging {
  val encoding = "UTF-8"
  val vmArgsKey = "java.rt.vmArgs"
  val vmNameKey = "java.property.java.vm.name"

  val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  implicit val _ = context.system.dispatchers.lookup(MetricsEndpoint.Dispatcher)

  val Jvm = com.twitter.jvm.Jvm()
  val id = com.twitter.jvm.Jvms.processId()

  private val runtime = Runtime.getRuntime()
  private var gcName: Option[String] = None

  import scala.concurrent.duration._

  val pushTimeout = FiniteDuration(10, TimeUnit.SECONDS)
  implicit val askTimeout = akka.util.Timeout(1, TimeUnit.SECONDS)

  override def preStart = {
    val snap = Jvm.snapCounters
    val vmArgs = snap(vmArgsKey)
    val vmName = snap(vmNameKey)
    log.info("\n ★ ★ ★ ★ ★ ★ \nJvmArgs: {}\n{}\n{}\n★ ★ ★ ★ ★ ★", vmName, vmArgs, org.openjdk.jol.vm.VM.current().details())
    val exp = "(.+)-XX:\\+Use(\\w+)GC(.+)".r
    gcName = vmArgs match {
      case exp(_, name, _) ⇒ Some(name)
      case _               ⇒ None
    }
  }

  override def receive: Receive = {
    case line: String ⇒
      onNext(ByteString(line, encoding))
      sender() ! true
    case req @ Request(n) ⇒
      log.info("JvmMetrics req {}", n)
      collect(n)
    case SubscriptionTimeoutExceeded ⇒
      log.info("JvmMetrics subscription timeout")
      (context stop self)
    case Cancel ⇒
      log.info("JvmMetrics canceled")
      (context stop self)
  }

  private def collect(n: Long): Unit = {
    if (isActive && totalDemand > 0 && n > 0) {
      val perfConters = Jvm.snap
      val metaspace = Jvm.metaspaceUsage
      val gcs = perfConters.lastGcs.map(i ⇒ s"${i.timestamp}: ${i.count}: [${i.name}: ${i.duration}]").mkString("\n")
      val heap = perfConters.heap
      val gcsHisto = heap.ageHisto.mkString(",")
      val heapSize = runtime.totalMemory / 1000000
      val heapMaxSize = runtime.maxMemory / 1000000
      val heapFreeSize = runtime.freeMemory / 1000000

      val mb = heap.allocated / 1000000

      val metasp = metaspace
        .map(m ⇒ s"Capacity:${m.capacity.inMegabytes} MaxCapacity:${m.maxCapacity.inMegabytes} Used:${m.used.inMegabytes} ").mkString(", ")

      val line = new StringBuilder().append(s"Process:${id.get}").append("\n")
        .append(s"Heap: Max:$heapMaxSize mb Size:$heapSize mb Used:${heapSize - heapFreeSize} mb ")
        .append(s"Allocated total:${mb} mb ").append("\n")
        .append("Metaspace: ").append(s"$metasp").append("\n").append(s"GC:$gcName")
        .append("\n").append(s"$gcs").append("\n").append(s"$gcsHisto").append("\n\n").toString()

      akka.pattern.after(pushTimeout, context.system.scheduler)(self.ask(line))
        .onComplete(_ ⇒ collect(n - 1))
    }
  }
}