package org.dweb_browser.pure.http

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.IFrom
import org.dweb_browser.helper.LateInit
import org.dweb_browser.helper.commonAsyncExceptionHandler
import org.dweb_browser.helper.toIpcUrl
import kotlin.coroutines.coroutineContext

/**
 * 代表 PureClientRequest，通常由请求的发起者进行构建
 */
data class PureClientRequest(
  override val href: String,
  override val method: PureMethod,
  override val headers: PureHeaders = PureHeaders(),
  override val body: IPureBody = IPureBody.Empty,
  override val channel: CompletableDeferred<PureChannel>? = null,
  override val from: Any? = null
) : PureRequest() {
  companion object {
    inline fun <reified T> fromJson(
      href: String,
      method: PureMethod,
      body: T,
      headers: PureHeaders = PureHeaders(),
      from: Any? = null
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
  override val from: Any? = null
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

sealed class PureRequest : PureUrl, IFrom {
  abstract val href: String
  abstract val method: PureMethod
  abstract val headers: PureHeaders
  abstract val body: IPureBody
  abstract val channel: CompletableDeferred<PureChannel>?

  override val url by lazy {
    href.toIpcUrl()
  }

  private val channelPreparer get() = this.channel ?: throw Exception("no support as channel");

  val hasChannel get() = this.channel != null
  suspend fun getChannel() = channelPreparer.await()
  suspend fun byChannel(
    by: suspend PureChannel.() -> Unit
  ): PureResponse {
    channelPreparer// check support
    CoroutineScope(coroutineContext + commonAsyncExceptionHandler).launch {
      getChannel().by()
    }
    return PureResponse(HttpStatusCode.SwitchingProtocols)
  }

  /**
   * 通常来说，客户端不能主动构建 Channel，需要由远端同意才能握手成功，但因为是Pure系列，所以它涵盖更加广义的场景
   * 因此这里的 toRemote 函数是通用于 PureClientRequest 与 PureServerRequest
   *
   * 并且本质上，Channel 只是一个抽象的通道，它的建立不代表什么，更重要的是服务端是否响应 101，这意味着这个通道将会带有意义，否则它应该被关闭
   */
  protected suspend fun toRemoteChannel(localeChannel: CompletableDeferred<PureChannel>?) =
    localeChannel?.let { remoteChannel ->
      CompletableDeferred<PureChannel>().also { localeChannel ->
        val job = CoroutineScope(coroutineContext + commonAsyncExceptionHandler).launch {
          localeChannel.complete(remoteChannel.await().reverse())
        }
        localeChannel.invokeOnCompletion { job.cancel() }
      }
    }
}
