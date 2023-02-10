package info.bagen.rust.plaoc.microService

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class HttpNMM {
    private val mmid: String = "https.sys.dweb"
    private val routers: Router = mutableMapOf()
    private val listenMap =  mutableMapOf</* host */ String, HttpListener>()
    private val internal = "jsProcess"
    init {
        embeddedServer(Netty, port = 25543) {
            routing {
                get("/listen") {
                    val port = call.request.queryParameters["port"]
                    if (port == null || port == "") {
                        call.respondText(DefaultErrorResponse(statusCode = 301,errorMessage = "not found request param port").toString())
                        return@get
                    }
                    println("https.sys.dweb#listen:$port")
                    createListen(port)
                }
            }
        }.start(wait = true)
    }

    private fun createListen(port:String):String {
        println("kotlin#LocalhostNMM createListen==> $mmid")
        val host = getHost(port)
        this.listenMap["$internal.$port"] = HttpListener(host)
        return host
    }


    private fun getHost(port: String):String {
        return "http://$internal.$port.$mmid";
    }
}

enum class Method(method: String = "GET") {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    OPTIONS( "OPTIONS")
}

fun rand(start: Int, end: Int): Int {
    require(start <= end) { "Illegal Argument" }
    return (start..end).random()
}

data class HttpRequestInfo(
    var http_req_id: Number,
    var url: String,
    var method: Method,
    var rawHeaders: MutableList<String> = mutableListOf()
)

data class HttpResponseInfo(
    var http_req_id: Number,
    var statusCode: Number,
    var headers: Map<String, String>,
    var body: Any
)
data class HttpListener(
    var host:String= ""
) {
    private val protocol = "https://"
    val origin = "$protocol${this.host}.${rand(0,25535)}.localhost"

    fun getAvailablePort():Number {
        return  25535
    }
}