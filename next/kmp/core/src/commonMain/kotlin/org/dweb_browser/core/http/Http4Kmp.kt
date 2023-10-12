package org.dweb_browser.core.http

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.module.HttpHandler
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.http.MatchMode
import org.dweb_browser.core.std.http.RouteConfig

class HttpRouter(private val mm: MicroModule) {
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

  /**
   * 允许跨域
   */
  fun cors(): HttpRouter {
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
    return this
  }

  /**
   * 接口私有化
   */
  fun private() = protected(setOf(mm.mmid))

  /**
   * 接口访问保护
   */
  fun protected(allows: Set<MMID>): HttpRouter {
    for ((key, handler) in routes) {
      val privateHandler: HttpHandler = { ctx ->
        if (!allows.contains(ctx.ipc.remote.mmid)) PureResponse(HttpStatusCode.Forbidden)
        else handler(ctx)
      }
      routes[key] = privateHandler
    }
    return this
  }
}


data class RoutingHttpHandler(val routeConfig: RouteConfig, val handler: HttpHandler)


class PathMethod(
  private val path: String, private val method: HttpMethod, private val matchMode: MatchMode
) {
  infix fun to(action: HttpHandler) = RoutingHttpHandler(
    RouteConfig(pathname = path, method = IpcMethod.from(method), matchMode = matchMode), action
  )
}

infix fun String.bind(method: HttpMethod) = PathMethod(this, method, MatchMode.FULL)
infix fun String.bindPrefix(method: HttpMethod) = PathMethod(this, method, MatchMode.PREFIX)
infix fun String.bindDwebDeeplink(action: HttpHandler) = RoutingHttpHandler(
  RouteConfig(
    dwebDeeplink = true, pathname = this, method = IpcMethod.GET, matchMode = MatchMode.FULL
  ), action
)
