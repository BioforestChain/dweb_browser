package org.dweb_browser.microservice.http

import io.ktor.http.HttpMethod
import io.ktor.http.fullPath
import org.dweb_browser.microservice.core.HttpHandler
import org.dweb_browser.microservice.ipc.helper.IpcMethod
import org.dweb_browser.microservice.ipc.helper.IpcRequest
import org.dweb_browser.microservice.sys.http.Gateway
import org.dweb_browser.microservice.sys.http.MatchMode

class HttpRouter {
  private val routes = mutableMapOf<Gateway.RouteConfig, HttpHandler>()

  fun addRoutes(vararg list: RoutingHttpHandler) {
    list.forEach {
      routes[it.routeConfig] = it.handler
    }
  }

  fun withFilter(request: IpcRequest): HttpHandler? {
    for (route in routes) {
      when (route.key.matchMode) {
        MatchMode.PREFIX -> if (route.key.method == request.method && request.uri.encodedPath.startsWith(
            route.key.pathname
          )
        ) {
          return route.value
        }

        MatchMode.FULL -> if (route.key.method == request.method && request.uri.encodedPath == route.key.pathname) {
          return route.value
        }
      }
    }

    return null
  }

  fun cors() {
    for ((key, handler) in routes) {
      val corsHandler: HttpHandler = { ctx ->
        handler(ctx).also {
          it.headers.apply {
            init("Access-Control-Allow-Credentials", "true")
            init("Access-Control-Allow-Origin", "*")
            init("Access-Control-Allow-Headers", "*")
            init("Access-Control-Allow-Methods", "*")
          }
        }
      }
      routes[key] = corsHandler
    }
  }
}


data class RoutingHttpHandler(val routeConfig: Gateway.RouteConfig, val handler: HttpHandler)


class PathMethod(private val path: String, private val method: HttpMethod) {
  infix fun to(action: HttpHandler): RoutingHttpHandler =
    RoutingHttpHandler(Gateway.RouteConfig(path, IpcMethod.from(method)), action)
}

infix fun String.bind(method: HttpMethod): PathMethod = PathMethod(this, method)
