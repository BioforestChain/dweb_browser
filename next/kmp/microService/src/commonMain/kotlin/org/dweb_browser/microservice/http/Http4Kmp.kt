package org.dweb_browser.microservice.http

import io.ktor.http.HttpMethod
import io.ktor.http.fullPath
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import org.dweb_browser.microservice.ipc.Ipc
import org.dweb_browser.microservice.ipc.helper.IpcMethod
import org.dweb_browser.microservice.ipc.helper.IpcRequest
import org.dweb_browser.microservice.sys.http.Gateway
import org.dweb_browser.microservice.sys.http.MatchMode

typealias HttpHandler = (request: PureRequest, ipc: Ipc?) -> PureResponse
class HttpRouter {
  private val routes = mutableMapOf<Gateway.RouteConfig, HttpHandler>()

  fun addRoutes(vararg list: RoutingHttpHandler) {
    list.forEach {
      routes.put(it.routeConfig, it.handler)
    }
  }

  fun withFilter(request: IpcRequest) : HttpHandler? {
    for (route in routes) {
      when(route.key.matchMode) {
        MatchMode.PREFIX -> if(route.key.method == request.method && request.uri.fullPath.startsWith(route.key.pathname)) {
          return route.value
        }
        MatchMode.FULL -> if(route.key.method == request.method && request.uri.fullPath == route.key.pathname) {
          return route.value
        }
      }
    }

    return null
  }
}

val router = HttpRouter()

data class RoutingHttpHandler(val routeConfig: Gateway.RouteConfig, val handler: HttpHandler)

fun routes(vararg list: RoutingHttpHandler) = router.addRoutes(*list)

class PathMethod(private val path: String, private val method: HttpMethod) {
  infix fun to(action: HttpHandler): RoutingHttpHandler =
    RoutingHttpHandler(Gateway.RouteConfig(path, IpcMethod.from(method)), action)
}

infix fun String.bind(method: HttpMethod): PathMethod = PathMethod(this, method)
