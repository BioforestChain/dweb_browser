package org.dweb_browser.pure.http

import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.LateInit

/**
 * 代表 PureClientRequest，通常由请求的发起者进行构建
 */
data class PureClientRequest(
  override val href: String,
  override val method: PureMethod,
  override val headers: PureHeaders = PureHeaders(),
  override val body: IPureBody = IPureBody.Empty,
  override val channel: CompletableDeferred<PureChannel>? = null,
  override val from: Any? = null,
) : PureRequest() {
  companion object {
    inline fun <reified T> fromJson(
      href: String,
      method: PureMethod,
      body: T,
      headers: PureHeaders = PureHeaders(),
      from: Any? = null,
    ) = PureClientRequest(
      href, method, headers.apply { init("Content-Type", "application/json") }, IPureBody.from(
        Json.encodeToString(body)
      ), from = from
    )
  }

  internal val server = LateInit<PureServerRequest>()
  suspend fun toServer() = server.getOrInit {
    PureServerRequest(
      href,
      method,
      headers,
      body,
      toRemoteChannel(channel),
      this@PureClientRequest
    ).apply { client.set(this@PureClientRequest) }
  }
}

