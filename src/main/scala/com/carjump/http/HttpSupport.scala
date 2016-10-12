package com.carjump.http

import java.time.format.DateTimeFormatter

import akka.http.scaladsl.model._
import akka.http.scaladsl.server._
import akka.util.ByteString

trait HttpSupport extends Directives {

  protected val latencyHeader = "Latency_Micro"
  protected val timeHeader = "Last_Updated_Time"
  protected val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

  def httpPath: String

  def withUri: Directive1[String] = extract(_.request.uri.toString)

  def notFound(error: String) = HttpResponse(StatusCodes.NotFound, entity = error)

  def internalError(error: String) = HttpResponse(StatusCodes.InternalServerError, entity = error)

  def success(resp: String) =
    HttpResponse(StatusCodes.OK, scala.collection.immutable.Seq[HttpHeader](),
      HttpEntity(ContentTypes.`text/plain(UTF-8)`, ByteString(resp)))

  def success(resp: String, hs: scala.collection.immutable.Seq[HttpHeader]) =
    HttpResponse(StatusCodes.OK, hs, HttpEntity(ContentTypes.`text/plain(UTF-8)`, ByteString(resp)))

}
