package org.dweb_browser.core.std.http

import io.ktor.http.URLBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.helper.getCompletedOrNull
import org.dweb_browser.pure.http.PureMethod

/// 对外提供一套建议的操作来创建、使用、维护这个http服务

@Serializable
data class DwebHttpServerOptions(
  val subdomain: String = "",
)


class HttpDwebServer(
  private val nmm: MicroModule.Runtime,
  private val httpIpc: Ipc,
  private val options: DwebHttpServerOptions,
) {
  suspend fun startHttpDwebServer(options: DwebHttpServerOptions): ServerStartResult {
    val urlString = URLBuilder("file://http.std.dweb/start").apply {
      parameters["subdomain"] = options.subdomain
    }.buildUnsafeString()

    return httpIpc.request(urlString).json<ServerStartResult>()
  }

  lateinit var startResult: ServerStartResult
    internal set

  suspend fun listenHttpDwebServer(
    startResult: ServerStartResult,
    routes: Array<CommonRoute> = arrayOf(
      CommonRoute(pathname = "", method = PureMethod.ALL_VALUES.keys.joinToString("|")),
    ),
    customServerIpc: Ipc? = null,
  ): Ipc {
    debugHttp("listen", nmm.mmid)
    val serverIpc =
      customServerIpc ?: httpIpc.fork(autoStart = true, startReason = "listenHttpDwebServer")
    serverIpc.request(URLBuilder("file://http.std.dweb/listen").apply {
      parameters["token"] = startResult.token
      parameters["routes"] = Json.encodeToString(routes)
    }.buildUnsafeString())
    return serverIpc
  }

  suspend fun closeHttpDwebServer(options: DwebHttpServerOptions) = httpIpc.request(
    URLBuilder("file://http.std.dweb/close").apply {
      parameters["subdomain"] = options.subdomain
    }.buildUnsafeString(),
  ).boolean()

  private val listenPo = CompletableDeferred<Ipc>()
  val listen = SuspendOnce {
    if (listenPo.isCompleted) {
      throw Exception("Listen method has been called more than once without closing.");
    }
    val streamIpc = listenHttpDwebServer(startResult)
    listenPo.complete(streamIpc)
    streamIpc
  }

  val close = SuspendOnce {
    val ipc = listenPo.getCompletedOrNull() ?: listenPo.await()
    if (ipc.isClosed) {
      return@SuspendOnce
    }
    runCatching {
      // 尝试发送关闭指令（对方也会将我进行关闭，但我仍然需要执行主动关闭，确保自己的资源正确释放，对方不释放是它自己的事情）
      closeHttpDwebServer(options)
    }.getOrElse {
      debugHttp("HttpDwebServer.close", "error", it)
    }
    // 主动关闭
    ipc.close()
  }
}

suspend fun MicroModule.Runtime.createHttpDwebServer(options: DwebHttpServerOptions): HttpDwebServer {
  val httpIpc = connect("http.std.dweb")
  return HttpDwebServer(this, httpIpc, options).also {
    it.startResult = it.startHttpDwebServer(options)
  }
}


