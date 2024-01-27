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

// 创建一个 httpServer 对象
private fun  httpServerListener(req: IncomingMessage, res: ServerResponse<*>) {
    req.url?.let { url ->
        when {
            url == "/"
                    || url.endsWith(".html")
                    || url.endsWith(".js")
                    || url.endsWith(".js.map")
                    || url.endsWith(".mjs")
                    || url.endsWith(".mjs.map")
                    || url.endsWith(".wasm")-> getAssets(req, res)
            // TODO: 添加继续分发的路由？？？
            else -> res.notFound()
        }
    }
}

private fun getFilenameByRequestUrl(req: IncomingMessage): String{
    val basePath = path.resolve("", "./kotlin")
    val subDomain = req.headers.host?.split(".localhost")?.get(0)?:""
    val pathStr = "./${subDomain.split(".").joinToString("/")}/${if(req.url == "/") "/index.html" else req.url}"
    return path.resolve(basePath, pathStr)
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

    suspend fun start(port: Int = 8888, listeningListener: ( Server<IncomingMessage, ServerResponse<*>>.() -> Unit)? = null): HttpServer{
        whenReady.await()
        return start(port, listeningListener)
    }
}


