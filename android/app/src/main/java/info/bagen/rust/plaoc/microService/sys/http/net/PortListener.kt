package info.bagen.rust.plaoc.microService.sys.http.net

import info.bagen.rust.plaoc.microService.helper.SimpleCallback
import info.bagen.rust.plaoc.microService.helper.SimpleSignal
import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import io.ktor.http.*
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

interface Router {
    val routes: MutableList<Request>
    val streamIpc: ReadableStreamIpc
}


class PortListener(
    val ipc: Ipc,
    val host: String,
    val origin: String
) {
    private val _routers = mutableSetOf<Router>();
    fun addRouter(router: Router): () -> Any {
        this._routers.add(router)
        return {
            this._routers.remove(router)
        }
    }

    private fun isBindMatchReq(pathname: String, method: Method): Pair<Router, Request>? {
        for (bind in this._routers) {
            for (pathMatcher in bind.routes) {
                if (isMatchReq(pathMatcher, pathname, method)) {
                    return Pair(bind, pathMatcher)
                }
            }
        }
        return null
    }

    /**
     * 接收 nodejs-web 请求
     * 将之转发给 IPC 处理，等待远端处理完成再代理响应回去
     */
    suspend fun hookHttpRequest(req: Request): Response? {
        val method = req.method
        val parsedUrl = req.uri
        println("hookHttpRequest==>method:$method,parsedUrl:$parsedUrl")
        val hasMatch = this.isBindMatchReq(parsedUrl.host, method);
        if (hasMatch == null) {
            return Response(Status.NOT_FOUND);
        }
        return null
    }

    private val destroySignal = SimpleSignal()
    fun onDestroy(cb: SimpleCallback) = destroySignal.listen { cb }

    suspend fun destroy() = destroySignal.emit()
}


interface ReqMatcher {
    val pathname: String;
    val matchMode: MatchMode
    var method: HttpMethod?
}

fun isMatchReq(
    matcher: Request,
    pathname: String,
    method: Method = Method.GET
): Boolean {
    val matchMethod = if (matcher.method == method) {
        matcher.method(method)
        true
    } else {
        false
    }
    println("PortListener#isMatchReq===>${matcher.equals("full")},${matcher.uri.path} ")
    val matchMode = if (matcher.equals("full")) {
        pathname == matcher.uri.path
    } else {
        if (matcher.equals("prefix")) {
            pathname.startsWith(matcher.uri.path)
        } else {
            false
        }
    }
    return matchMethod && matchMode
};

enum class MatchMode(type: String) {
    full("full"),
    prefix("prefix")
}