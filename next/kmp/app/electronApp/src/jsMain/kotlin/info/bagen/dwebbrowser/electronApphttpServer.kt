package info.bagen.dwebbrowser

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import node.buffer.BufferEncoding
import node.fs.readFile
import node.http.IncomingMessage
import node.http.Server
import node.http.ServerResponse
import org.dweb_browser.js_backend.http.HttpServer
import org.dweb_browser.js_backend.http.MatchPattern
import org.dweb_browser.js_backend.http.Method
import org.dweb_browser.js_backend.http.Route

// 创建一个 httpServer 对象
private fun  httpServerListener(req: IncomingMessage, res: ServerResponse<*>) {
    when {
        req.url?.endsWith(".html") == true || req.url?.endsWith(".js") == true || req.url?.endsWith(
            ".js.map"
        ) == true || req.url?.endsWith(".mjs") == true || req.url?.endsWith(".mjs.map") == true -> getAssets(
            req,
            res
        )

        else -> {
            console.error(
                """
                    还有处理的路由url == ${req.url}
                    at httpServerListener
                    at class BrowserViewModel()
                    at viewModel.kt
                """.trimIndent()
            )
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
        val str = readFile(fileName, BufferEncoding.utf8)
        setContentType(fileName, res)
        res.appendHeader("Content-Type", "text/html")
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
    }
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


