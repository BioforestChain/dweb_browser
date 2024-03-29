package org.dweb_browser.core.std.http

import io.ktor.http.URLBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.ReadableStreamIpc
import org.dweb_browser.core.ipc.kotlinIpcPool
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.buildUnsafeString
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureStreamBody

/// 对外提供一套建议的操作来创建、使用、维护这个http服务

@Serializable
data class DwebHttpServerOptions(
  val subdomain: String = "",
) {
}

suspend fun MicroModule.startHttpDwebServer(options: DwebHttpServerOptions): HttpNMM.ServerStartResult {
  val urlString = URLBuilder("file://http.std.dweb/start").apply {
    parameters["subdomain"] = options.subdomain
  }.buildUnsafeString()

  return this.nativeFetch(
    urlString
  ).json<HttpNMM.ServerStartResult>()
}


suspend fun MicroModule.listenHttpDwebServer(
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
    CommonRoute(pathname = "", method = PureMethod.TRACE)
  )
): ReadableStreamIpc {
  debugHttp("listen", microModule.mmid)
  val httpIpc = this.connect("http.std.dweb")
  val streamIpc =
    kotlinIpcPool.create(
      "http-server/${startResult.urlInfo.host}",
      httpIpc.remote,
    ) {
      nativeFetch(
        PureClientRequest(
          URLBuilder("file://http.std.dweb/listen").apply {
            parameters["token"] = startResult.token
            parameters["routes"] = Json.encodeToString(routes)
          }.buildUnsafeString(),
          PureMethod.POST,
          body = PureStreamBody(it.input.stream)
        )
      ).stream()
    }
  this.addToIpcSet(streamIpc)
  return streamIpc
}


suspend fun MicroModule.closeHttpDwebServer(options: DwebHttpServerOptions) =
  this.nativeFetch(
    URLBuilder("file://http.std.dweb/close").apply {
      parameters["subdomain"] = options.subdomain
    }.buildUnsafeString(),
  ).boolean()

class HttpDwebServer(
  private val nmm: MicroModule,
  private val options: DwebHttpServerOptions,
  val startResult: HttpNMM.ServerStartResult
) {
  private val listenPo = PromiseOut<ReadableStreamIpc>()
  val listen = SuspendOnce<ReadableStreamIpc> {
    if (listenPo.isFinished) {
      throw Exception("Listen method has been called more than once without closing.");
    }
    val streamIpc = nmm.listenHttpDwebServer(
      nmm,
      startResult
    )
    listenPo.resolve(streamIpc)
    return@SuspendOnce streamIpc
  }

  val close = SuspendOnce {
    listenPo.waitPromise().close()// 主动关闭
    nmm.closeHttpDwebServer(options) // 并且发送关闭指令（对方也会将我进行关闭，但我仍然需要执行主动关闭，确保自己的资源正确释放，对方不释放是它自己的事情）
  }
}

suspend fun MicroModule.createHttpDwebServer(options: DwebHttpServerOptions) =
  HttpDwebServer(this, options, startHttpDwebServer(options))

