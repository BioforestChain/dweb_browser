package info.bagen.rust.plaoc.microService.network

import info.bagen.rust.plaoc.microService.ipc.Ipc
import info.bagen.rust.plaoc.microService.ipc.ipcWeb.ReadableStreamIpc
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.HttpMethod

interface Router {
    val routes: MutableList<ReqMatcher>
    val streamIpc: ReadableStreamIpc
}


class PortListener(
    val ipc: Ipc,
    val host: String,
    val origin: String
) {

    private val _routers = mutableSetOf<Router>();
    fun addRouter(router: Router): () -> Any{
        this._routers.add(router)
        return {
            this._routers.remove(router)
        }
    }

    private fun isBindMatchReq(pathname: String, method:  HttpMethod): Pair<Router, ReqMatcher>? {
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
    fun hookHttpRequest(req: ApplicationRequest, res:  ApplicationResponse) {
        val url = req.location() ?: "/";
        val method = req.httpMethod
        val parsedUrl = Url(url)
        println("hookHttpRequest==>url:$url,method:$method,parsedUrl:$parsedUrl")

        val hasMatch = this.isBindMatchReq(parsedUrl.host, method);
        if (hasMatch == null) {
            DefaultErrorResponse(404, "no found");
            return;
        }
    }
}


interface ReqMatcher {
    val pathname: String;
    val matchMode: MatchMode
    var method: HttpMethod?
}

fun isMatchReq(
    matcher: ReqMatcher,
    pathname: String,
    method: HttpMethod = HttpMethod.Get
): Boolean {
    val matchMethod = if ((matcher.method == method || matcher.method == null)) {
        matcher.method = method
        true
    } else {
        false
    }
    val matchMode = if (matcher.matchMode.equals("full")) {
        pathname == matcher.pathname
    } else {
        if (matcher.matchMode.equals("prefix")) {
            pathname.startsWith(matcher.pathname)
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