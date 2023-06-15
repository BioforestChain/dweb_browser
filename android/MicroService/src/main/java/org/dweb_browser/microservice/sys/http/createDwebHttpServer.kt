package org.dweb_browser.microservice.sys.http

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.help.boolean
import org.dweb_browser.microservice.help.gson
import org.dweb_browser.microservice.help.json
import org.dweb_browser.microservice.help.stream
import org.dweb_browser.microservice.help.suspendOnce
import org.dweb_browser.microservice.ipc.ReadableStreamIpc
import org.dweb_browser.microservice.ipc.message.IpcMethod
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
        Uri.of("file://http.sys.dweb/start")
            .query("port", options.port.toString())
            .query("subdomain", options.subdomain)
    ).json<HttpNMM.ServerStartResult>(HttpNMM.ServerStartResult::class.java)


suspend fun MicroModule.listenHttpDwebServer(
  startResult: HttpNMM.ServerStartResult,
  routes: Array<Gateway.RouteConfig>
) =
    ReadableStreamIpc(this, "http-server/${startResult.urlInfo.host}").also {
        it.bindIncomeStream(
            this.nativeFetch(
                Request(
                    Method.POST,
                    Uri.of("file://http.sys.dweb/listen")
                        .query("host", startResult.urlInfo.host)
                        .query("token", startResult.token)
                        .query("routes", gson.toJson(routes))
                ).body(it.stream)
            ).stream()
        )
    }


suspend fun MicroModule.closeHttpDwebServer(options: DwebHttpServerOptions) =
    this.nativeFetch(
        Uri.of("file://http.sys.dweb/close")
            .query("port", options.port.toString())
            .query("subdomain", options.subdomain)
    ).boolean()

class HttpDwebServer(
  private val nmm: MicroModule,
  private val options: DwebHttpServerOptions,
  val startResult: HttpNMM.ServerStartResult
) {
    suspend fun listen(
        routes: Array<Gateway.RouteConfig> = arrayOf(
          Gateway.RouteConfig(pathname = "", method = IpcMethod.GET),
          Gateway.RouteConfig(pathname = "", method = IpcMethod.POST),
          Gateway.RouteConfig(pathname = "", method = IpcMethod.PUT),
          Gateway.RouteConfig(pathname = "", method = IpcMethod.DELETE)
        ),
    ) = runBlockingCatching {
      val po = PromiseOut<ReadableStreamIpc>()
      GlobalScope.launch(ioAsyncExceptionHandler) {
        val streamIpc = nmm.listenHttpDwebServer(startResult, routes)
        po.resolve(streamIpc)
      }
      po.waitPromise()
    }.getOrThrow()


    val close = suspendOnce { nmm.closeHttpDwebServer(options) }
}

suspend fun MicroModule.createHttpDwebServer(options: DwebHttpServerOptions) =
    HttpDwebServer(this, options, startHttpDwebServer(options))

