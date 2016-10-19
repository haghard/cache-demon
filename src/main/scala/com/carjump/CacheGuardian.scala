package com.carjump

import akka.actor._
import com.carjump.http.ReqParams
import net.ceedubs.ficus.Ficus._
import scala.concurrent.duration._
import akka.actor.SupervisorStrategy.Directive
import akka.pattern.{ Backoff, BackoffSupervisor }

object CacheGuardian {
  def props = Props[CacheGuardian].withDispatcher(Cache.DispatcherName)
}

class CacheGuardian extends Actor with ActorLogging {
  val conf = context.system.settings.config

  val url = conf.as[String]("ws.url")
  val pref = conf.as[String]("ws.path")
  val pullInterval = conf.as[FiniteDuration]("ws.interval")

  val minBackoffInterval = conf.as[FiniteDuration]("ws.min_backoff")
  val maxBackoffInterval = conf.as[FiniteDuration]("ws.max_backoff")

  val decider: PartialFunction[Throwable, Directive] = {
    case ex: Throwable ⇒
      log.error(ex, "Cache has failed unexpectedly. We will try to recreate it later")
      akka.actor.SupervisorStrategy.Stop
  }

  val props = BackoffSupervisor.props(
    Backoff.onStop(childProps = Cache.props(url, pref, pullInterval), childName = "cache",
      minBackoff = minBackoffInterval, maxBackoff = maxBackoffInterval, randomFactor = 0.2)
      .withSupervisorStrategy(OneForOneStrategy()(decider.orElse(SupervisorStrategy.defaultStrategy.decider))))

  val fetcher = context.actorOf(props)

  override def receive: Receive = {
    case msg: ReqParams ⇒ (fetcher forward msg)
  }
}