package info.bagen.rust.plaoc.microService

import android.os.Build
import info.bagen.rust.plaoc.microService.network.Http1Server
import info.bagen.rust.plaoc.microService.network.Http1Server.Companion.PORT
import info.bagen.rust.plaoc.microService.network.HttpListener
import info.bagen.rust.plaoc.microService.network.PortListener
import info.bagen.rust.plaoc.microService.route.jsProcessRoute
import info.bagen.rust.plaoc.microService.route.webViewRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.*

@Serializable
data class Origin(val origin: String)


class Gateway(
    val listener: PortListener,
    val host: String,
    val token: String
) {

}

class HttpNMM : NativeMicroModule() {
    override val mmid: String = "http.sys.dweb"
    private val http1Server = Http1Server()

    public override fun _bootstrap() {
        http1Server.createServer()
    }

    fun createListen(port: String): String {
        println("kotlin#LocalhostNMM createListen==> $mmid")
        val host = getHost(port)

        val token = "xxxx"
//        val listener = PortListener(ipc, origin, origin)
//        val gateway = Gateway(listener, host, token)
//        this.http1Server.tokenMap[token] = gateway
//        this.http1Server.gatewayMap[host] = gateway
        return host
    }

    private fun getHost(port: String): String {
        return "http://internal.js.sys.dweb-$port.localhost:${PORT}/js-process";
    }

    fun closeServer() {
        http1Server.closeServer()
    }
}

fun Application.moduleRouter() {
    routing {
        get("/") {
            call.respondText(
                text = "Hello!! You are here in ${Build.MODEL}",
                contentType = ContentType.Text.Plain
            )
        }
        jsProcessRoute()
        webViewRoute()
    }
}

