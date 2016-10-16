package com.carjump.http

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server._
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings }
import akka.stream.scaladsl._
import akka.util.ByteString

object MetricsEndpoint {
  val Dispatcher = "akka.metrics-dispatcher"
}

class MetricsEndpoint(override val httpPath: String = "metrics")(implicit system: ActorSystem) extends HttpSupport {
  import MetricsEndpoint._

  implicit val mat = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withDispatcher(Dispatcher)
      .withInputBuffer(1, 1))
  //.withSupervisionStrategy(decider))(context.system)

  //This allows us to have just one source actor and many subscribers
  val metricsSource =
    Source.actorPublisher[ByteString](Props[JvmMetrics].withDispatcher(Dispatcher))
      .toMat(BroadcastHub.sink(bufferSize = 32))(Keep.right).run()

  //Ensure that the Broadcast output is dropped if there are no listening parties.
  metricsSource.runWith(Sink.ignore)

  //This would create one actor for one client
  //Source.actorPublisher[ByteString](Props[JvmMetricsClient].withDispatcher(Dispatcher))

  override val route: Route =
    (get & path(httpPath / "jvm")) {
      withUri { url ⇒
        complete {
          HttpResponse(entity = HttpEntity.Chunked.fromData(ContentTypes.`text/plain(UTF-8)`, metricsSource))
        }
      }
    }
}