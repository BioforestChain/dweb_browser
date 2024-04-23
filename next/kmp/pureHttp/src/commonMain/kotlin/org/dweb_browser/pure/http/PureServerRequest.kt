package org.dweb_browser.pure.http

import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.helper.LateInit

/**
 * 通常代表 PureServerRequest，这里由请求的接收者进行构建
 * 通常只能由 PureRequest.toIncome 得来
 */
data class PureServerRequest(
  override val href: String,
  override val method: PureMethod,
  override val headers: PureHeaders = PureHeaders(),
  override val body: IPureBody = IPureBody.Empty,
  override val channel: CompletableDeferred<PureChannel>? = null,
  override val from: Any? = null,
) : PureRequest() {
  internal val client = LateInit<PureClientRequest>()
  suspend fun toClient() = client.getOrInit {
    PureClientRequest(
      href,
      method,
      headers,
      body,
      toRemoteChannel(channel),
      this@PureServerRequest
    ).apply { server.set(this@PureServerRequest) }
  }
}