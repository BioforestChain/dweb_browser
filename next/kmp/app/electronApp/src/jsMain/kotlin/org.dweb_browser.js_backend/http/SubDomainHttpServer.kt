package org.dweb_browser.js_backend.http

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import node.http.IncomingMessage
import node.http.Server
import node.http.ServerResponse
import node.fs.readFile
import node.fs.ReadFileBufferAsyncOptions
import node.http.RequestListener
import node.url.parse

external val __dirname: String

// 创建一个 httpServer 对象
private fun getFilenameByRequestUrl(req: IncomingMessage): String{
    val subDomain = req.headers.host?.split(".localhost")?.get(0)?:""
    val pathStr = "./${subDomain.split(".").joinToString("/")}/${if(req.url == "/") "/index.html" else req.url}"
    return path.resolve(__dirname, pathStr)
}

private fun getAssets(req: IncomingMessage, res: ServerResponse<*>) {
    CoroutineScope(Dispatchers.Unconfined).launch {
        val fileName = getFilenameByRequestUrl(req)
        console.log("filename: ", fileName)
        setContentType(fileName, res)
        val options: ReadFileBufferAsyncOptions? = null
        val str = readFile(fileName, options)
        res.end(str)

    }
}

private fun setContentType(fileName: String, res: ServerResponse<*>) {
    when {
        fileName.endsWith(".html") -> res.appendHeader("Content-type", "text/html")
        fileName.endsWith(".js") -> res.appendHeader("Content-Type", "application/javascript")
        fileName.endsWith(".mjs") -> res.appendHeader("Content-Type", "application/javascript")
        fileName.endsWith(".js.map") -> res.appendHeader("Content-Type", "application/json")
        fileName.endsWith(".mjs.map") -> res.appendHeader("Content-Type", "application/json")
        fileName.endsWith(".wasm") -> res.appendHeader("Content-Type", "application/wasm")
    }
    res.appendHeader("Accept-Ranges", "bytes")
}

class SubDomainHttpServer(
    // example: demo.compose.app
    val subDomain: String
){

    val scope = CoroutineScope(Dispatchers.Unconfined)
    val whenReady = CompletableDeferred<HttpServer>()
    lateinit var httpServer: HttpServer
    private val router = Router()
    init {
        scope.launch {
            httpServer = HttpServer.createHttpServer().await()
            whenReady.complete(httpServer)
            httpServer.routeAdd(
                Route(subDomain,"/", Method.GET, MatchPattern.PREFIX, ::httpServerListener),
            )
        }
    }

    suspend  fun start(listeningListener: ( Server<IncomingMessage, ServerResponse<*>>.() -> Unit)? = null): HttpServer{
        whenReady.await()
        return httpServer.start(listeningListener)
    }

    /**
     * @return String
     * - example demo.compose.app.localhost
     */
    fun getHostName() = "${subDomain}.${httpServer.serverGetHostName()}"

    /**
     * @return String
     * - example demo.compose.app.localhost:port
     */
    fun getHost() = "${subDomain}.${httpServer.serverGetHost()}"

    /**
     * @return String
     * - example http://demo.compose.app.localhost:port
     */
    fun getBaseUrl() = "${httpServer.serverGetProtocol()}//${getHost()}"

    private fun  httpServerListener(req: IncomingMessage, res: ServerResponse<*>) {
        req.url?.let { url ->
            console.log("走到了这里", req.url)
            when {
                url == "/"
                        || url.endsWith(".html")
                        || url.endsWith(".js")
                        || url.endsWith(".js.map")
                        || url.endsWith(".mjs")
                        || url.endsWith(".mjs.map")
                        || url.endsWith(".wasm")-> getAssets(req, res)
                else -> {
                    val reqMethod = req.method?:throw(Throwable("""
                        req.method == null
                        req.method: ${req.method}
                        at requestListener
                        at HttpServer
                    """.trimIndent()))

                    val route = req.url?.let { parse(it,false) }?.pathname?.let { reqPath ->
                        router.getAllRoutes().values.firstOrNull {
                            it.hasMatch(reqPath, reqMethod, subDomain)
                        }
                    }
                    when(route){
                        null -> res.notFound()
                        else -> route(req, res)
                    }
                }
            }
        }
    }

    fun addRoute(
        path: String,
        method: Method,
        matchPattern: MatchPattern,
        listeningListener: RequestListener<IncomingMessage, ServerResponse<*>>
    ){
        router.add(Route(subDomain, path, method,matchPattern, listeningListener))
    }
}





