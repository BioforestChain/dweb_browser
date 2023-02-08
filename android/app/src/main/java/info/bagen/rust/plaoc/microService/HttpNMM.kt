package info.bagen.rust.plaoc.microService

class HttpNMM : NativeMicroModule() {
    override val mmid: String = "https.sys.dweb"
    private val routers: Router = mutableMapOf()
    private val listenMap =  mutableMapOf</* host */ String, HttpListener>();
    init {
        // 注册路由
        routers["/listen"] = put@{ option ->
            val port = option["port"] ?: return@put "Error: not found port"
            return@put createListen(port)
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

    private fun createListen(it:String):String {
        println("kotlin#LocalhostNMM createListen==> $mmid")
        return getHost(it)
    }

    override fun bootstrap(routerTarget:String, options: NativeOptions): Any? {
        println("kotlin#LocalhostNMM bootstrap==> ${options["mainCode"]}  ${options["origin"]}")
        // 导航到自己的路由
        if (routers[routerTarget] == null) {
            return "localhost.sys.dweb route not found for $routerTarget"
        }
        return routers[routerTarget]?.let { it->it(options) }
    }

    private fun getHost(port: String):String {
        return "https://internal.$port.$mmid";
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

data class HttpListener(
    val origin:String = ""
) {

}