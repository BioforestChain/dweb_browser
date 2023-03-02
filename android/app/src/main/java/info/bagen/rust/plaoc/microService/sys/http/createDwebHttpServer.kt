package info.bagen.rust.plaoc.microService.sys.http

import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.*
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.IpcMethod
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.sys.dns.nativeFetch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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


suspend fun MicroModule.listenHttpDwebServer(token: String, routes: Array<Gateway.RouteConfig>) =
    ReadableStreamIpc(this, IPC_ROLE.CLIENT).also {
        it.bindIncomeStream(
            this.nativeFetch(
                Request(
                    Method.POST,
                    Uri.of("file://http.sys.dweb/listen")
                        .query("token", token)
                        .query("routes", gson.toJson(routes))
                ).body(it.stream)
            ).stream(),
            "http-server"
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
    ) = runBlocking {
        val po = PromiseOut<ReadableStreamIpc>()
        GlobalScope.launch {
            val streamIpc = nmm.listenHttpDwebServer(startResult.token, routes)
            po.resolve(streamIpc)
        }
        po.waitPromise()
    }


    val close = suspendOnce { nmm.closeHttpDwebServer(options) }
}

suspend fun MicroModule.createHttpDwebServer(options: DwebHttpServerOptions) =
    HttpDwebServer(this, options, startHttpDwebServer(options))

