package org.dweb_browser.microservice.help


import org.http4k.core.Method
import org.http4k.core.then
import org.http4k.filter.AllowAll
import org.http4k.filter.CorsPolicy
import org.http4k.filter.OriginPolicy
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler

fun RoutingHttpHandler.cors(): RoutingHttpHandler {
  return ServerFilters.Cors(
    CorsPolicy(
      OriginPolicy.AllowAll(), listOf("*"), Method.values().toList()
    )
  ).then(this)
}
