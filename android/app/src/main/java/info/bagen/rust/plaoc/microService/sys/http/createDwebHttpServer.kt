package info.bagen.rust.plaoc.microService.sys.http

import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.boolean
import info.bagen.rust.plaoc.microService.helper.json
import info.bagen.rust.plaoc.microService.helper.stream
import info.bagen.rust.plaoc.microService.helper.suspendOnce
import info.bagen.rust.plaoc.microService.ipc.IPC_ROLE
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import info.bagen.rust.plaoc.microService.sys.http.net.nativeFetch
import org.http4k.core.Uri
import org.http4k.core.query


/// 对外提供一套建议的操作来创建、使用、维护这个http服务

data class DwebServerOptions(
    val port: Int,
    val subdomain: String,
) {
    constructor(
        port: Int? = HttpNMM.DwebServer.PORT,
        subdomain: String? = "",
    ) : this(port ?: HttpNMM.DwebServer.PORT, subdomain ?: "")
}

data class HttpDwebServerInfo(
    val origin: String,
    val token: String
)

suspend fun MicroModule.startHttpDwebServer(options: DwebServerOptions) =
    this.nativeFetch(
        Uri.of("file://http.sys.dweb/start")
            .query("port", options.port.toString())
            .query("subdomain", options.subdomain)
    ).json<HttpDwebServerInfo>(HttpDwebServerInfo::class.java)


suspend fun MicroModule.listenHttpDwebServer(token: String) =
    ReadableStreamIpc(this, IPC_ROLE.CLIENT).also {
        it.bindIncomeStream(
            this.nativeFetch(
                Uri.of("file://http.sys.dweb/listen")
                    .query("token", token)
            ).stream()
        )
    }


suspend fun MicroModule.closeHttpDwebServer(options: DwebServerOptions) =
    this.nativeFetch(
        Uri.of("file://http.sys.dweb/close")
            .query("port", options.port.toString())
            .query("subdomain", options.subdomain)
    ).boolean()

class HttpDwebServer(private val nmm: MicroModule, private val options: DwebServerOptions) {
    val start = suspendOnce { nmm.startHttpDwebServer(options) }
    val listen = suspend { nmm.listenHttpDwebServer(start().token) }
    val close = suspendOnce { nmm.closeHttpDwebServer(options) }
}

suspend fun MicroModule.createHttpDwebServer(options: DwebServerOptions) =
    HttpDwebServer(this, options).also { it.start() }

