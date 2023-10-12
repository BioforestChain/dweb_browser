package org.dweb_browser.core.std.http

import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.helper.IpcMethod

@Serializable
data class RouteConfig(
  val dwebDeeplink: Boolean = false,
  val pathname: String,
  val method: IpcMethod,
  val matchMode: MatchMode = MatchMode.PREFIX
) {
  private fun methodMatcher(request: PureRequest) = request.method == method
  private fun protocolMatcher(request: PureRequest) =
    if (dwebDeeplink) request.href.startsWith("dweb:") else true

  private fun pathnameMatcher(request: PureRequest) = if (dwebDeeplink) {
    request.href.substring("dweb://".length).split('?', limit = 2)[0]
  } else {
    request.url.encodedPath
  }.let { target ->
    when (matchMode) {
      MatchMode.PREFIX -> target.startsWith(pathname)
      MatchMode.FULL -> target == pathname
    }
  }

  fun isMatch(request: PureRequest): Boolean {
    return methodMatcher(request)
        //
        && protocolMatcher(request)
        //
        && pathnameMatcher(request)
  }
}