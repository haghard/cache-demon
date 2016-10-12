package com.carjump

import java.time.ZonedDateTime

package object http {

  trait CacheResponseBody {
    def latency: Long

    def error: Option[String]

    def lastUpdated: ZonedDateTime
  }

  trait ReqParams

}