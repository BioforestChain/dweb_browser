package org.dweb_browser.core.std.http

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.http.router.HttpHandlerChain
import org.dweb_browser.core.http.router.RouteHandler
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureRequest

@Serializable
class CommonRoute(
  val dwebDeeplink: Boolean = false,
  override val pathname: String,
  val method: String,
  val matchMode: MatchMode = MatchMode.PREFIX,
) : IRoute {
  constructor(
    dwebDeeplink: Boolean = false,
    pathname: String,
    method: PureMethod,
    matchMode: MatchMode = MatchMode.PREFIX,
  ) : this(dwebDeeplink, pathname, method.method, matchMode)

  @Transient
  val methods = method.uppercase().split(Regex("[|,\\s]+")).toSet()

  private fun methodMatcher(request: PureRequest) = methods.contains(request.method.method)
  private fun protocolMatcher(request: PureRequest) =
    if (dwebDeeplink) request.href.startsWith("dweb:") else true

  private fun pathnameMatcher(request: PureRequest) = if (dwebDeeplink) {
    request.href.substring("dweb://".length).split('?', limit = 2)[0]
  } else {
    request.url.encodedPath
  }.let { target ->
    PathRoute.isMatch(target, pathname, matchMode)
  }

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
  override val pathname: String,
  val matchMode: MatchMode = MatchMode.PREFIX,
) : IRoute {
  companion object {
    fun isMatch(request: PureRequest, config_pathname: String, config_matchMode: MatchMode) =
      isMatch(request.url.encodedPath, config_pathname, config_matchMode)

    fun isMatch(request_pathname: String, config_pathname: String, config_matchMode: MatchMode) =
      when (config_matchMode) {
        MatchMode.PREFIX -> request_pathname.startsWith(config_pathname)
        MatchMode.FULL -> request_pathname.trimEnd('/') == config_pathname
      }
  }

  override fun isMatch(request: PureRequest) =
    isMatch(request, pathname, matchMode)
}

@Serializable
data class DuplexRoute(override val pathname: String, val matchMode: MatchMode = MatchMode.PREFIX) :
  IRoute {
  override fun isMatch(request: PureRequest): Boolean {
    val matched = request.hasChannel && PathRoute.isMatch(request, pathname, matchMode)
//    debugHttp("isMatch") {
//      "hasChannel=${request.hasChannel} isMatch=$matched pathname=${request.url.encodedPath}=$pathname matchMode=$matchMode"
//    }
    return matched
  }
}

interface IRoute {
  val pathname: String?
  fun isMatch(request: PureRequest): Boolean
}