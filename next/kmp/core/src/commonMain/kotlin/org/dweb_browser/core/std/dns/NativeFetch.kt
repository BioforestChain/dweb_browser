package org.dweb_browser.core.std.dns

import io.ktor.http.Url
import org.dweb_browser.core.help.AdapterManager
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.defaultHttpPureClient

typealias FetchAdapter = suspend (remote: MicroModule.Runtime, request: PureClientRequest) -> PureResponse?

val debugFetch = Debugger("fetch")

val debugFetchFile = Debugger("fetch-file")

/**
 * file:/// => /usr & /sys as const
 * file://file.std.dweb/ => /home & /tmp & /share as userData
 */
val nativeFetchAdaptersManager = NativeFetchAdaptersManager()

class NativeFetchAdaptersManager : AdapterManager<FetchAdapter>() {

  private var client = defaultHttpPureClient

  class HttpFetch(private val manager: NativeFetchAdaptersManager) {
    val client get() = manager.client
    suspend operator fun invoke(request: PureClientRequest) = fetch(request)
    suspend operator fun invoke(url: String, method: PureMethod = PureMethod.GET) =
      fetch(PureClientRequest(method = method, href = url))

    val fetch = client::fetch
  }

  val httpFetch = HttpFetch(this)
}

val httpFetch = nativeFetchAdaptersManager.httpFetch

suspend fun MicroModule.Runtime.nativeFetch(request: PureClientRequest): PureResponse {
  for (fetchAdapter in nativeFetchAdaptersManager.adapters) {
    val response = fetchAdapter(this, request)
    if (response != null) {
      return response
    }
  }
  return nativeFetchAdaptersManager.httpFetch(request)
}

suspend inline fun MicroModule.Runtime.nativeFetch(url: Url) = nativeFetch(PureMethod.GET, url)

suspend inline fun MicroModule.Runtime.nativeFetch(url: String) = nativeFetch(PureMethod.GET, url)

suspend inline fun MicroModule.Runtime.nativeFetch(method: PureMethod, url: Url) =
  nativeFetch(PureClientRequest(url.toString(), method))

suspend inline fun MicroModule.Runtime.nativeFetch(method: PureMethod, url: String) =
  nativeFetch(PureClientRequest(url, method))
