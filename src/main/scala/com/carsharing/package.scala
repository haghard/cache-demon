package com

import akka.actor.ActorRef
import com.carsharing.http.{ReqParams, CacheResponseBody}
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import akka.pattern.AskTimeoutException

import scala.concurrent.{ExecutionContext, Future}

package object carsharing {

  implicit def funcToRunnable(f: () ⇒ Unit) = new Runnable {
    override def run() = f()
  }

  implicit def asFiniteDuration(d: java.time.Duration) =
    scala.concurrent.duration.Duration.fromNanos(d.toNanos)

  case class ValidationException(message: String) extends Exception(message)


  trait AskSupport {
    import akka.pattern.ask

    implicit def askTimeout: akka.util.Timeout

    def queryCache[T <: CacheResponseBody](message: ReqParams, cache: ActorRef)(implicit ec: ExecutionContext, tag: ClassTag[T]): Future[cats.data.Ior[String, T]] =
      cache.ask(message).mapTo[T].map { response =>
        response.error.fold(cats.data.Ior.right(response)) { error =>
          throw ValidationException(error)
        }
      }.recoverWith {
        case ex: ClassCastException ⇒ Future.successful(cats.data.Ior.left(s"Could'n cast type: ${ex.getMessage}"))
        case ex: AskTimeoutException ⇒ Future.successful(cats.data.Ior.left(s"Request timeout: ${ex.getMessage}"))
        case ex: ValidationException => Future.successful(cats.data.Ior.left(s"Validation error: ${ex.getMessage}"))
        case NonFatal(e) => //Doesn't swallow stack overflow and out of memory errors
          Future.successful(cats.data.Ior.left(s"Unexpected error: ${e.getMessage}"))
      }
  }
}