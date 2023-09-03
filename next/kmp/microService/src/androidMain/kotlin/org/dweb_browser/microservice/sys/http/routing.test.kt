package org.dweb_browser.microservice.sys.http

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.routes

fun main() {
  val app = routes(Method.GET to { request ->
    Response(Status.OK).body(request.uri.toString())
  })

  val res = app(Request(Method.GET, "/"))

  println(res.status)
  println(res.bodyString())
}