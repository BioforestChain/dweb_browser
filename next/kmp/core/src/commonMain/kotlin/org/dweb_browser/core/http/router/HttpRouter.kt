package org.dweb_browser.core.http.router

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.toPure
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.http.MatchMode
import org.dweb_browser.core.std.http.RouteConfig
import org.dweb_browser.helper.remove

class HttpRouter(private val mm: MicroModule) {
  private val routes = mutableMapOf<RouteConfig, HttpHandlerChain>()

  fun addRoutes(vararg list: RoutingHttpHandler) {
    list.forEach {
      routes[it.routeConfig] = it.handler
    }
  }

  fun addRoutes(rs: Map<RouteConfig, HttpHandlerChain>) {
    rs.forEach {
      routes[it.key] = it.value
    }
  }

  fun removeRoutes(rs: Map<RouteConfig, HttpHandlerChain>) {
    rs.forEach {
      routes.remove(it.key, it.value)
    }
  }

  fun withFilter(request: IpcRequest): HttpHandlerChain? {
    for ((config, handler) in routes) {
      if (config.isMatch(request.toPure())) {
        return handler
      }
    }

    return null
  }

  private val cors_handler: MiddlewareHttpHandler = { next ->
    val res = next()
    res.headers.run {
      init("Access-Control-Allow-Credentials", "true")
      init("Access-Control-Allow-Origin", "*")
      init("Access-Control-Allow-Headers", "*")
      init("Access-Control-Allow-Methods", "*")
    }
    res
  }

  /**
   * 允许跨域
   */
  fun cors(): HttpRouter {
    for (handler in routes.values) {
      handler.use(cors_handler)
    }
    return this
  }


  companion object {
    private val protected_handlers = mutableMapOf<String, MiddlewareHttpHandler>()
    private fun getProtectedHandler(allows: Set<MMID>) =
      protected_handlers.getOrPut(allows.sorted().joinToString(",")) {
        { next ->
          if (!allows.contains(ipc.remote.mmid)) PureResponse(HttpStatusCode.Forbidden)
          else next()
        }
      }
  }

  /**
   * 接口私有化
   */
  fun private() = protected(setOf(mm.mmid))

  /**
   * 接口访问保护
   */
  fun protected(allows: Set<MMID>): HttpRouter {
    val protectedHandler = getProtectedHandler(allows)
    for (handler in routes.values) {
      handler.use(protectedHandler)
    }
    return this
  }

  operator fun plus(other: HttpRouter) = HttpRouter(mm).also {
    it.addRoutes(this.routes)
    it.addRoutes(other.routes)
  }

  operator fun plusAssign(other: HttpRouter) {
    if (other != this) {
      addRoutes(other.routes)
    }
  }

  operator fun minusAssign(other: HttpRouter) {
    if (other == this) {
      routes.clear()
    } else {
      removeRoutes(other.routes)
    }
  }
}


data class RoutingHttpHandler(val routeConfig: RouteConfig, val handler: HttpHandlerChain)


class PathMethod(
  private val path: String, private val method: HttpMethod, private val matchMode: MatchMode
) {
  infix fun to(action: HttpHandlerChain) = RoutingHttpHandler(
    RouteConfig(pathname = path, method = IpcMethod.from(method), matchMode = matchMode), action
  )
}

infix fun String.bind(method: HttpMethod) = PathMethod(this, method, MatchMode.FULL)
infix fun String.bindPrefix(method: HttpMethod) = PathMethod(this, method, MatchMode.PREFIX)
infix fun String.bindDwebDeeplink(action: HttpHandler) = bindDwebDeeplink(action.toChain())
infix fun String.bindDwebDeeplink(action: HttpHandlerChain) = RoutingHttpHandler(
  RouteConfig(
    dwebDeeplink = true, pathname = this, method = IpcMethod.GET, matchMode = MatchMode.FULL
  ), action
)
