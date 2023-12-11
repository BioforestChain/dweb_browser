package org.dweb_browser.core.std.http

import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.isWebSocket
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.http.router.HttpHandlerChain
import org.dweb_browser.core.http.router.RouteHandler
import org.dweb_browser.core.ipc.helper.IpcMethod

@Serializable
data class CommonRoute(
  val dwebDeeplink: Boolean = false,
  val pathname: String,
  val method: IpcMethod,
  val matchMode: MatchMode = MatchMode.PREFIX
) : IRoute {
  private fun methodMatcher(request: PureRequest) = request.method == method
  private fun protocolMatcher(request: PureRequest) =
    if (dwebDeeplink) request.href.startsWith("dweb:") else true

  private fun pathnameMatcher(request: PureRequest) =
    PathRoute.isMatch(request, pathname, matchMode, dwebDeeplink)

  override fun isMatch(request: PureRequest): Boolean {
    return methodMatcher(request)
        //
        && protocolMatcher(request)
        //
        && pathnameMatcher(request)
  }

  infix fun by(action: HttpHandlerChain) = RouteHandler(this, action)

}

@Serializable
data class PathRoute(
  val pathname: String,
  val matchMode: MatchMode = MatchMode.PREFIX,
  val dwebDeeplink: Boolean = false
) : IRoute {
  companion object {
    fun isMatch(
      request: PureRequest,
      pathname: String,
      matchMode: MatchMode,
      dwebDeeplink: Boolean
    ) = if (dwebDeeplink) {
      request.href.substring("dweb://".length).split('?', limit = 2)[0]
    } else {
      request.url.encodedPath
    }.let { target ->
      when (matchMode) {
        MatchMode.PREFIX -> target.startsWith(pathname)
        MatchMode.FULL -> target == pathname
      }
    }
  }

  override fun isMatch(request: PureRequest) =
    isMatch(request, pathname, matchMode, dwebDeeplink)
}

@Serializable
data class DuplexRoute(val pathname: String, val matchMode: MatchMode = MatchMode.PREFIX) :
  IRoute {
  override fun isMatch(request: PureRequest) =
    request.isWebSocket() && PathRoute.isMatch(request, pathname, matchMode, false)
}

interface IRoute {
  fun isMatch(request: PureRequest): Boolean
}