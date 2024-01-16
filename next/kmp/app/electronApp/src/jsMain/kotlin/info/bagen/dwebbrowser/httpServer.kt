package info.bagen.dwebbrowser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import node.buffer.BufferEncoding
import node.fs.readFile
import node.http.IncomingMessage
import node.http.ServerResponse
import org.dweb_browser.js_backend.http.HttpServer

// 创建一个 httpServer 对象
private fun  httpServerListener(req: IncomingMessage, res: ServerResponse<*>) {
    console.log("req.url: ${req.url}")
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

val httpServerDeferred = HttpServer.createHttpServer(::httpServerListener)

