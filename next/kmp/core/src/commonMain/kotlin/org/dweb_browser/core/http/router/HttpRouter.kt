package org.dweb_browser.core.http.router

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.http.CommonRoute
import org.dweb_browser.core.std.http.DuplexRoute
import org.dweb_browser.core.std.http.IRoute
import org.dweb_browser.core.std.http.MatchMode
import org.dweb_browser.core.std.http.PathRoute
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.core.std.permission.PERMISSION_ID
import org.dweb_browser.core.std.permission.ext.queryPermissions
import org.dweb_browser.helper.remove
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureRequest
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.initCors

class HttpRouter(private val mm: MicroModule.Runtime, val host: String) {
  private val routes = mutableMapOf<IRoute, HttpHandlerChain>()

  /**
   * 权限表，这是共享的
   * 在一次回话中，校验结果暂时不需要释放
   * TODO 这里应该做成支持 双工订阅，从而允许让 mm 介入权限授权的部分。
   * 等于说 permission.std 只是一个”吵架大楼“，provider先做一些预先过滤动作，过滤完了，再将要提示的信息告知用户，用户授权那就通过
   * 如果 provider 没有进入”吵架大楼“，那么就按默认的预设来告知用户
   *
   * 这样这里的这个map对象就能一直是最新的，并且是更新了这个 map 后，permission 再做授权响应回去
   */
  private var mmidPermissionStatus =
    mapOf<PERMISSION_ID, Map<MMID /* = String */, AuthorizationStatus>>()
  private val checkLock = Mutex()

  private fun getPermissionStatus(permissions: List<PERMISSION_ID>, mmid: MMID) =
    mmidPermissionStatus.map {
      if (permissions.contains(it.key)) {
        it.value[mmid]
      } else AuthorizationStatus.UNKNOWN
    }

  private suspend fun checkPermission(permissions: List<PERMISSION_ID>, mmid: MMID) =
    checkLock.withLock {
      // 由于 permission.std.dweb 有权限设置界面（也就是后台权限管理），所以需要每次判断都强制请求最新数据
      mmidPermissionStatus = mm.queryPermissions(permissions)
      val status = getPermissionStatus(permissions, mmid)
      !(status.any { it != AuthorizationStatus.GRANTED }) // 如果有任何权限是非授权的，返回 false
    }

  fun addRoutes(vararg list: RouteHandler) {
    list.forEach { routeHandler ->
      val permissionIds = if (routeHandler.route.pathname == null) {
        listOf()
      } else {
        mm.dweb_permissions.filter { permission ->
          permission.routes.any { route ->
            "file://$host${routeHandler.route.pathname}".startsWith(route)
          }
        }.map { it.pid.toString() }
      }
      routes[routeHandler.route] = if (permissionIds.isNotEmpty()) {
        HttpHandlerChain {
          if (!checkPermission(permissionIds, ipc.remote.mmid)) {
            // permissionIds 包含了 dweb_permissions 中所有的需要授权的 pid 列表
            PureResponse(
              HttpStatusCode.Unauthorized,
              body = IPureBody.Companion.from(permissionIds.joinToString(",")) // 返回需要授权的 permissionIds
            )
          } else {
            /// 原始响应
            routeHandler.handler.invoke(this)
          }
        }
      } else {
        routeHandler.handler
      }
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

  suspend fun withFilter(request: PureRequest): HttpHandlerChain? {
    for ((config, handler) in routes) {
      if (config.isMatch(request)) {
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
      res.headers.initCors()
      res
    }

    private val corsRoute: Pair<IRoute, HttpHandlerChain> = object : IRoute {
      override val pathname: String? = null

      override fun isMatch(request: PureRequest) = request.method == PureMethod.OPTIONS
    } to HttpHandlerChain {
      PureResponse().apply { headers.initCors() }
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

  operator fun plus(other: HttpRouter) = HttpRouter(mm, host).also {
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


data class RouteHandler(val route: IRoute, val handler: HttpHandlerChain) {
  init {
    handler.ctx?.addRoutes(this)
  }
}

infix fun String.bind(method: PureMethod) =
  CommonRoute(pathname = this, method = method, matchMode = MatchMode.FULL)

infix fun String.bindPrefix(method: PureMethod) =
  CommonRoute(pathname = this, method = method, matchMode = MatchMode.PREFIX)

infix fun String.bindDwebDeeplink(action: HttpHandler) = bindDwebDeeplink(action.toChain())
infix fun String.bindDwebDeeplink(action: HttpHandlerChain) = RouteHandler(
  CommonRoute(
    dwebDeeplink = true, pathname = this, method = "GET|POST|PUT|DELETE", matchMode = MatchMode.FULL
  ), action
)

infix fun String.by(action: HttpHandlerChain) =
  RouteHandler(PathRoute(this, MatchMode.FULL), action)

infix fun String.byPrefix(action: HttpHandlerChain) =
  RouteHandler(PathRoute(this, MatchMode.PREFIX), action)

infix fun String.byChannel(
  by: suspend IChannelHandlerContext.(PureChannelContext) -> Unit,
) = RouteHandler(
  DuplexRoute(this, MatchMode.FULL),
  HttpHandlerChain {
    request.byChannel {
      val ctx = ChannelHandlerContext(
        this@HttpHandlerChain,
        this,
        start(),
      )
      ctx.by(ctx.pureChannelContext)
    }
  })
