package org.dweb_browser.core.http

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.ipc.helper.IpcHeaders
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.helper.toIpcUrl
import kotlin.coroutines.coroutineContext

data class PureRequest(
  val href: String,
  val method: IpcMethod,
  val headers: IpcHeaders = IpcHeaders(),
  val body: IPureBody = IPureBody.Empty,
  private val channel: CompletableDeferred<PureChannel>? = null,
  val from: Any? = null
) : PureUrl {
  override val url by lazy {
    href.toIpcUrl()
  }

  companion object {
    inline fun <reified T> fromJson(
      href: String,
      method: IpcMethod,
      body: T,
      headers: IpcHeaders = IpcHeaders(),
      from: Any? = null
    ) = PureRequest(
      href, method, headers.apply { init("Content-Type", "application/json") }, IPureBody.from(
        Json.encodeToString(body)
      ), from = from
    )
  }

  private val channelPreparer get() = this.channel ?: throw Exception("no support as channel");

  internal fun initChannel(channel: PureChannel) {
    channelPreparer.complete(channel)
  }

  val hasChannel get() = this.channel != null
  suspend fun getChannel() = channelPreparer.await()
  suspend fun byChannel(
    by: suspend PureChannel.() -> Unit
  ): PureResponse {
    channelPreparer// check support
    CoroutineScope(coroutineContext).launch {
      getChannel().by()
    }
    return PureResponse(HttpStatusCode.SwitchingProtocols)
  }
}

fun IpcRequest.toPure() = PureRequest(url, method, headers, body.raw)
