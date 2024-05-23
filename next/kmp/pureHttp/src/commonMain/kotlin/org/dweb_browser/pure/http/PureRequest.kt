package org.dweb_browser.pure.http

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.dweb_browser.helper.IFrom
import org.dweb_browser.helper.commonAsyncExceptionHandler
import org.dweb_browser.helper.toIpcUrl
import kotlin.coroutines.coroutineContext

sealed class PureRequest : PureUrl, IFrom {
  abstract val href: String
  abstract val method: PureMethod
  abstract val headers: PureHeaders
  abstract val body: IPureBody
  abstract val channel: CompletableDeferred<PureChannel>?

  override val url by lazy {
    href.toIpcUrl()
  }

  val isWebSocket get() = isWebSocket(this.method, this.headers)

  private val channelPreparer get() = this.channel ?: throw Exception("no support as channel");

  val hasChannel get() = this.channel != null
  suspend fun getChannel() = channelPreparer.await()
  suspend fun byChannel(by: suspend PureChannel.() -> Unit): PureResponse {
    channelPreparer// check support
    CoroutineScope(coroutineContext + commonAsyncExceptionHandler).launch(start = CoroutineStart.UNDISPATCHED) {
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
          val channel = remoteChannel.await()
          localeChannel.complete(channel.reverse())
        }
        localeChannel.invokeOnCompletion { job.cancel() }
      }
    }
}