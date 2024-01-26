package info.bagen.dwebbrowser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import node.http.IncomingMessage
import node.http.Server
import node.http.ServerResponse
import node.fs.readFile
import org.dweb_browser.js_backend.http.HttpServer
import org.dweb_browser.js_backend.http.MatchPattern
import org.dweb_browser.js_backend.http.Method
import org.dweb_browser.js_backend.http.Route
import node.fs.ReadFileBufferAsyncOptions
import org.dweb_browser.js_backend.http.notFound

// 创建一个 httpServer 对象
private fun  httpServerListener(req: IncomingMessage, res: ServerResponse<*>) {
    req.url?.let { url ->
        when {
            url.endsWith(".html")
                    || url.endsWith(".js")
                    || url.endsWith(".js.map")
                    || url.endsWith(".mjs")
                    || url.endsWith(".mjs.map")
                    || url.endsWith(".wasm")-> getAssets(req, res)
            else -> res.notFound()
        }
    }
}

private fun getFilenameByRequestUrl(req: IncomingMessage): String{
    val basePath = path.resolve("", "./kotlin")
    val pathStr = ".${req.url}"
    return path.resolve(basePath, pathStr)
}

private fun getAssets(req: IncomingMessage, res: ServerResponse<*>) {
    CoroutineScope(Dispatchers.Unconfined).launch {
        val fileName = getFilenameByRequestUrl(req)
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

class ElectronAppHttpServer(){
    val scope = CoroutineScope(Dispatchers.Unconfined)
    val whenReady = CompletableDeferred<HttpServer>()
    lateinit var httpServer: HttpServer
    init {
        scope.launch {
            httpServer = HttpServer.createHttpServer().await()
            whenReady.complete(httpServer)
            httpServer.routeAdd(
                Route("/demoReactApp", Method.GET, MatchPattern.PREFIX, ::httpServerListener),
            )
            httpServer.routeAdd(
                Route("/demoComposeApp", Method.GET, MatchPattern.PREFIX, ::httpServerListener),
            )
        }
    }

    suspend  fun start(listeningListener: ( Server<IncomingMessage, ServerResponse<*>>.() -> Unit)? = null): HttpServer{
        whenReady.await()
        return httpServer.start(listeningListener)
    }

    suspend fun start(port: Int = 8888, listeningListener: ( Server<IncomingMessage, ServerResponse<*>>.() -> Unit)? = null): HttpServer{
        whenReady.await()
        return start(port, listeningListener)
    }
}


