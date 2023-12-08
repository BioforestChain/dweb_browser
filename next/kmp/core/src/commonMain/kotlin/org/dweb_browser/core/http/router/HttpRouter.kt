package org.dweb_browser.core.http.router

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.PureResponse
import org.dweb_browser.core.http.toPure
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.http.CommonRoute
import org.dweb_browser.core.std.http.DuplexRoute
import org.dweb_browser.core.std.http.IRoute
import org.dweb_browser.core.std.http.MatchMode
import org.dweb_browser.core.std.http.PathRoute
import org.dweb_browser.helper.remove

class HttpRouter(private val mm: MicroModule) {
  private val routes = mutableMapOf<IRoute, HttpHandlerChain>()

  fun addRoutes(vararg list: RouteHandler) {
    list.forEach {
      routes[it.route] = it.handler
    }
  }

  fun addRoutes(rs: Map<IRoute, HttpHandlerChain>) {
    rs.forEach {
      routes[it.key] = it.value
    }
  }

  fun removeRoutes(rs: Map<IRoute, HttpHandlerChain>) {
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

  /**
   * 允许跨域
   */
  fun cors(): HttpRouter {
    routes += corsRoute
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


    private val cors_handler: MiddlewareHttpHandler = { next ->
      val res = next()
      res.headers.cors()
      res
    }

    private fun IpcHeaders.cors() {
      init("Access-Control-Allow-Credentials", "true")
      init("Access-Control-Allow-Origin", "*")
      init("Access-Control-Allow-Headers", "*")
      init("Access-Control-Allow-Methods", "*")
    }

    private val corsRoute: Pair<IRoute, HttpHandlerChain> = object : IRoute {
      override fun isMatch(request: PureRequest) = request.method == IpcMethod.OPTIONS
    } to HttpHandlerChain {
      PureResponse().apply { headers.cors() }
    }
  }

  /**
   * 接口私有化
   */
  fun private() = protected(setOf(mm.mmid))

  /**
   * 接口访问保护
   */
  fun protected(allows: MMID) = protected(setOf(allows))
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


data class RouteHandler(val route: IRoute, val handler: HttpHandlerChain)

infix fun String.bind(method: HttpMethod) = bind(IpcMethod.from(method))

infix fun String.bind(method: IpcMethod) =
  CommonRoute(pathname = this, method = method, matchMode = MatchMode.FULL)

infix fun String.bindPrefix(method: HttpMethod) = bindPrefix(IpcMethod.from(method))
infix fun String.bindPrefix(method: IpcMethod) =
  CommonRoute(pathname = this, method = method, matchMode = MatchMode.PREFIX)

infix fun String.bindDwebDeeplink(action: HttpHandler) = bindDwebDeeplink(action.toChain())
infix fun String.bindDwebDeeplink(action: HttpHandlerChain) = RouteHandler(
  CommonRoute(
    dwebDeeplink = true, pathname = this, method = IpcMethod.GET, matchMode = MatchMode.FULL
  ), action
)

infix fun String.by(action: HttpHandlerChain) =
  RouteHandler(PathRoute(this, MatchMode.FULL), action)

infix fun String.byPrefix(action: HttpHandlerChain) =
  RouteHandler(PathRoute(this, MatchMode.PREFIX), action)

infix fun String.byDuplex(action: HttpHandlerChain) =
  RouteHandler(DuplexRoute(this, MatchMode.FULL), action)
