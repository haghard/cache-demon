package com

import akka.actor.ActorRef
import akka.pattern.AskTimeoutException
import com.carjump.http.{CacheResponseBody, ReqParams}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.control.NonFatal

package object carjump {

  implicit def funcToRunnable(f: () ⇒ Unit) = new Runnable {
    override def run() = f()
  }

  implicit def asFiniteDuration(d: java.time.Duration) =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)

  case class ValidationException(message: String) extends Exception(message)


  trait AskSupport {

    import akka.pattern.ask

    implicit def askTimeout: akka.util.Timeout

    def queryCache[T <: CacheResponseBody](message: ReqParams, cache: ActorRef)(implicit ec: ExecutionContext, tag: ClassTag[T]): Future[cats.data.Xor[String, T]] =
      cache.ask(message).mapTo[T].map { response =>
        response.error.fold(cats.data.Xor.right(response)) { error =>
          throw ValidationException(error)
        }
      }.recoverWith {
        case ex: ClassCastException ⇒ Future.successful(cats.data.Xor.left(s"Could'n cast type: ${ex.getMessage}"))
        case ex: AskTimeoutException ⇒ Future.successful(cats.data.Xor.left(s"Request timeout: ${ex.getMessage}"))
        case ex: ValidationException => Future.successful(cats.data.Xor.left(s"Validation error: ${ex.getMessage}"))
        case NonFatal(e) => //Doesn't swallow stack overflow and out of memory errors
          Future.successful(cats.data.Xor.left(s"Unexpected error: ${e.getMessage}"))
      }
  }
}