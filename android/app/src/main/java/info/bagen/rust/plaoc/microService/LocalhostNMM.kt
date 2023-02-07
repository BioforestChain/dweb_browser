package info.bagen.rust.plaoc.microService

class LocalhostNMM : NativeMicroModule() {
    override val mmid: String = "localhost.sys.dweb"
    val routers: Router = mutableMapOf()

    init {
        // 注册路由
        routers["/listen"] = put@{
            return@put true
        }
        routers["/evalJavascript"] = put@{
            return@put true
        }
        routers["/request/on"] = put@{
            return@put true
        }
        routers["/response/emit"] = put@{
            return@put true
        }
        routers["/unregister"] = put@{
            return@put true
        }
    }

}

enum class Method(method: String = "GET") {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    DELETE("DELETE"),
    OPTIONS( "OPTIONS")
}

data class HttpRequestInfo(
    val http_req_id: Number,
    val url: String,
    val method: Method,
    val rawHeaders: MutableList<String> = mutableListOf()
)

data class HttpResponseInfo(
    val http_req_id: Number,
    val statusCode: Number,
    val headers: Map<String, String>,
    val body: Any
)

class HttpListener() {

}