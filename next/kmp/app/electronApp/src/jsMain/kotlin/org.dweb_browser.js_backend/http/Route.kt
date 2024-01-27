package org.dweb_browser.js_backend.http

import node.http.IncomingMessage
import node.http.RequestListener
import node.http.ServerResponse
import org.dweb_browser.js_backend.http.MatchPattern
import org.dweb_browser.js_backend.http.Method

class Route(
    val subDomain: String,
    val path: String,
    val method: Method,
    val matchPattern: MatchPattern,
    val listener: RequestListener<IncomingMessage, ServerResponse<*>>
){

    operator fun invoke(req: IncomingMessage, res: ServerResponse<*>) = listener(req, res)
    fun hasMatch(reqPath: String,  reqMethod: String, sd: String): Boolean{
        return if(matchPattern === MatchPattern.FULL){
            path == reqPath && method.value == reqMethod && subDomain == sd
        }else{
            reqPath.startsWith(path) && method.value == reqMethod && subDomain == sd
        }
    }
}