package org.dweb_browser.core.std.http

import io.ktor.http.URLBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.pure.http.PureMethod

/// 对外提供一套建议的操作来创建、使用、维护这个http服务

@Serializable
data class DwebHttpServerOptions(
  val subdomain: String = "",
) {
}

suspend fun MicroModule.Runtime.startHttpDwebServer(options: DwebHttpServerOptions): HttpNMM.ServerStartResult {
  val urlString = URLBuilder("file://http.std.dweb/start").apply {
    parameters["subdomain"] = options.subdomain
  }.buildUnsafeString()

  return this.nativeFetch(
    urlString
  ).json<HttpNMM.ServerStartResult>()
}


suspend fun MicroModule.Runtime.listenHttpDwebServer(
  microModule: IMicroModuleManifest,
  startResult: HttpNMM.ServerStartResult,
  routes: Array<CommonRoute> = arrayOf(
    CommonRoute(pathname = "", method = PureMethod.GET),
    CommonRoute(pathname = "", method = PureMethod.POST),
    CommonRoute(pathname = "", method = PureMethod.PUT),
    CommonRoute(pathname = "", method = PureMethod.DELETE),
    CommonRoute(pathname = "", method = PureMethod.OPTIONS),
    CommonRoute(pathname = "", method = PureMethod.PATCH),
    CommonRoute(pathname = "", method = PureMethod.HEAD),
    CommonRoute(pathname = "", method = PureMethod.CONNECT),
    CommonRoute(pathname = "", method = PureMethod.TRACE),
  ),
  customServerIpc: Ipc? = null,
): Ipc {
  debugHttp("listen", microModule.mmid)
  val httpIpc = this.connect("http.std.dweb")
  val serverIpc =
    customServerIpc ?: httpIpc.fork(autoStart = true, startReason = "listenHttpDwebServer")
  serverIpc.request(URLBuilder("file://http.std.dweb/listen").apply {
    parameters["token"] = startResult.token
    parameters["routes"] = Json.encodeToString(routes)
  }.buildUnsafeString())
  return serverIpc
}


suspend fun MicroModule.Runtime.closeHttpDwebServer(options: DwebHttpServerOptions) =
  this.nativeFetch(
    URLBuilder("file://http.std.dweb/close").apply {
      parameters["subdomain"] = options.subdomain
    }.buildUnsafeString(),
  ).boolean()

class HttpDwebServer(
  private val nmm: MicroModule.Runtime,
  private val options: DwebHttpServerOptions,
  val startResult: HttpNMM.ServerStartResult,
) {
  private val listenPo = PromiseOut<Ipc>()
  val listen = SuspendOnce {
    if (listenPo.isFinished) {
      throw Exception("Listen method has been called more than once without closing.");
    }
    val streamIpc = nmm.listenHttpDwebServer(
      nmm,
      startResult
    )
    listenPo.resolve(streamIpc)
    streamIpc
  }

  val close = SuspendOnce {
    listenPo.waitPromise().close()// 主动关闭
    nmm.closeHttpDwebServer(options) // 并且发送关闭指令（对方也会将我进行关闭，但我仍然需要执行主动关闭，确保自己的资源正确释放，对方不释放是它自己的事情）
  }
}

suspend fun MicroModule.Runtime.createHttpDwebServer(options: DwebHttpServerOptions) =
  HttpDwebServer(this, options, startHttpDwebServer(options))

