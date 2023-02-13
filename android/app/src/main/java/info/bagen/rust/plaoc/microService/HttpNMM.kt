package info.bagen.rust.plaoc.microService

import android.os.Build
import info.bagen.rust.plaoc.microService.network.Http1Server
import info.bagen.rust.plaoc.microService.network.HttpListener
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


    private fun bootstrap() {
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
        application.environment.log.info("Server is started rootPath:${application.environment.rootPath}")
    }
    routing {
        get("/") {
            call.respondText(
                text = "Hello!! You are here in ${Build.MODEL}",
                contentType = ContentType.Text.Plain
            )
        }
        get("/listen/{port}") {
            val port = call.parameters["port"]
            if (port == null || port == "") {
               return@get call.respondText(
                    DefaultErrorResponse(
                        statusCode = 403,
                        errorMessage = "not found request param port"
                    ).toString()
                )
            }
            println("https.sys.dweb#listen:$port,${Origin(httpNMM.createListen(port))}")

            call.respondText(httpNMM.createListen(port))
//          return@get call.respond(Origin(httpNMM.createListen(port)))
        }
        get("/create-process") { }
    }
}

