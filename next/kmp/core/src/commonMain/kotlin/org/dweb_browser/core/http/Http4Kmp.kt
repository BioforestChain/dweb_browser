package org.dweb_browser.core.http

import io.ktor.http.HttpMethod
import org.dweb_browser.core.module.HttpHandler
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.std.http.RouteConfig

class HttpRouter {
  private val routes = mutableMapOf<RouteConfig, HttpHandler>()

  fun addRoutes(vararg list: RoutingHttpHandler) {
    list.forEach {
      routes[it.routeConfig] = it.handler
    }
  }

  fun withFilter(request: IpcRequest): HttpHandler? {
    for ((config, handler) in routes) {
      if (config.isMatch(request.toPure())) {
        return handler
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


data class RoutingHttpHandler(val routeConfig: RouteConfig, val handler: HttpHandler)


class PathMethod(private val path: String, private val method: HttpMethod) {
  infix fun to(action: HttpHandler) = RoutingHttpHandler(
    RouteConfig(pathname = path, method = IpcMethod.from(method)), action
  )
}

infix fun String.bind(method: HttpMethod) = PathMethod(this, method)
infix fun String.bindDwebDeeplink(action: HttpHandler) = RoutingHttpHandler(
  RouteConfig(dwebDeeplink = true, pathname = this, method = IpcMethod.GET), action
)
