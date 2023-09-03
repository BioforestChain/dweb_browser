package org.dweb_browser.microservice.sys.http

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.bodyJson
import org.dweb_browser.microservice.help.boolean
import org.dweb_browser.microservice.help.stream
import org.dweb_browser.microservice.help.suspendOnce
import org.dweb_browser.microservice.help.types.IMicroModuleManifest
import org.dweb_browser.microservice.ipc.ReadableStreamIpc
import org.dweb_browser.microservice.ipc.helper.IpcMethod
import org.dweb_browser.microservice.sys.dns.nativeFetch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.query

/// 对外提供一套建议的操作来创建、使用、维护这个http服务

data class DwebHttpServerOptions(
  val port: Int,
  val subdomain: String,
) {
  constructor(
    port: Int? = 80,
    subdomain: String? = "",
  ) : this(port ?: 80, subdomain ?: "")
}

suspend fun MicroModule.startHttpDwebServer(options: DwebHttpServerOptions) =
  this.nativeFetch(
    Uri.of("file://http.std.dweb/start")
      .query("port", options.port.toString())
      .query("subdomain", options.subdomain)
  ).bodyJson<HttpNMM.ServerStartResult>()


suspend fun MicroModule.listenHttpDwebServer(
  microModule: IMicroModuleManifest,
  startResult: HttpNMM.ServerStartResult,
  routes: Array<Gateway.RouteConfig> = arrayOf(
    Gateway.RouteConfig(pathname = "", method = IpcMethod.GET),
    Gateway.RouteConfig(pathname = "", method = IpcMethod.POST),
    Gateway.RouteConfig(pathname = "", method = IpcMethod.PUT),
    Gateway.RouteConfig(pathname = "", method = IpcMethod.DELETE),
    Gateway.RouteConfig(pathname = "", method = IpcMethod.OPTIONS),
    Gateway.RouteConfig(pathname = "", method = IpcMethod.PATCH),
    Gateway.RouteConfig(pathname = "", method = IpcMethod.HEAD),
    Gateway.RouteConfig(pathname = "", method = IpcMethod.CONNECT),
    Gateway.RouteConfig(pathname = "", method = IpcMethod.TRACE)
  )
): ReadableStreamIpc {
  val streamIpc = ReadableStreamIpc(microModule, "http-server/${startResult.urlInfo.host}").also {
    it.bindIncomeStream(
      this.nativeFetch(
        Request(
          Method.POST,
          Uri.of("file://http.std.dweb/listen")
            .query("token", startResult.token)
            .query("routes", Json.encodeToString(routes))
        ).body(it.stream)
      ).stream()
    )
  }
  this.addToIpcSet(streamIpc)
  return streamIpc
}


suspend fun MicroModule.closeHttpDwebServer(options: DwebHttpServerOptions) =
  this.nativeFetch(
    Uri.of("file://http.std.dweb/close")
      .query("port", options.port.toString())
      .query("subdomain", options.subdomain)
  ).boolean()

class HttpDwebServer(
  private val nmm: MicroModule,
  private val options: DwebHttpServerOptions,
  val startResult: HttpNMM.ServerStartResult
) {
  private val listenPo = PromiseOut<ReadableStreamIpc>()
  val listen = suspendOnce {
    if (listenPo.isFinished) {
      throw Exception("Listen method has been called more than once without closing.");
    }
    val streamIpc = nmm.listenHttpDwebServer(
      nmm,
      startResult
    )
    listenPo.resolve(streamIpc)
    return@suspendOnce streamIpc
  }


  val close = suspendOnce {
    listenPo.waitPromise().close()// 主动关闭
    nmm.closeHttpDwebServer(options) // 并且发送关闭指令（对方也会将我进行关闭，但我仍然需要执行主动关闭，确保自己的资源正确释放，对方不释放是它自己的事情）
  }
}

suspend fun MicroModule.createHttpDwebServer(options: DwebHttpServerOptions) =
  HttpDwebServer(this, options, startHttpDwebServer(options))

