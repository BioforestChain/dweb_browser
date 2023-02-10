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