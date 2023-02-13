package info.bagen.rust.plaoc.microService.network

import info.bagen.rust.plaoc.microService.moduleRouter
import io.ktor.server.jetty.*
import io.ktor.server.application.*
import io.ktor.server.engine.*


class Http1Server  {
    fun createServer() {
        println("http1Server#start")
        embeddedServer(Jetty, port = 24433, module = Application::module).start(wait = true)
    }
}

fun Application.module() {
    moduleRouter()
    modulePlugin()
}

fun Application.modulePlugin() {
    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/shutdown"
        exitCodeSupplier = { 0 }
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
