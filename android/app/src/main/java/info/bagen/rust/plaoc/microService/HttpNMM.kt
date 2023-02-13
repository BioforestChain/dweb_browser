package info.bagen.rust.plaoc.microService

import android.os.Build
import info.bagen.rust.plaoc.microService.network.Http1Server
import info.bagen.rust.plaoc.microService.network.HttpListener
import info.bagen.rust.plaoc.microService.route.jsProcessRoute
import info.bagen.rust.plaoc.microService.route.webViewRoute
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.*

@Serializable
data class Origin(val origin: String)

val httpNMM = HttpNMM()


class HttpNMM {
    private val mmid: String = "http.sys.dweb"
    private val listenMap = mutableMapOf</* host */ String, HttpListener>()
    private val internal = "internal"
    private val http1 = Http1Server()
    val jsMicroModule = JsMicroModule()
    val bootNMM = BootNMM()
    val multiWebViewNMM = MultiWebViewNMM()


     fun bootstrap() {
        http1.createServer()
    }

    fun createListen(port: String): String {
        println("kotlin#LocalhostNMM createListen==> $mmid")
        val host = getHost(port)
        this.listenMap["$internal.$port"] = HttpListener(host)
        return host
    }


    private fun getHost(port: String): String {
        return "http://$internal.$port.$mmid";
    }

    fun closeServer() {

    }
}

fun Application.moduleRouter() {
    environment.monitor.subscribe(ApplicationStarted) { application ->
        println("Server is started,${application.environment.rootPath}")
    }
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

