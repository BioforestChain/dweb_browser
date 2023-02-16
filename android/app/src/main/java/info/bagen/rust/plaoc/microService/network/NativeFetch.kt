package info.bagen.rust.plaoc.microService.network

import info.bagen.libappmgr.network.base.BASE_URL
import info.bagen.rust.plaoc.microService.MicroModule
import info.bagen.rust.plaoc.microService.NativeMicroModule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.client.OkHttp
import org.http4k.core.Method
import org.http4k.core.Uri

val apiService = HttpClient(CIO) {
    install(ContentNegotiation) { // 引入数据转换插件
        gson()
    }
    install(Logging)
    {
        // logger = Logger.SIMPLE // 用于显示本机请求信息
        level = LogLevel.ALL
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 5000
        requestTimeoutMillis = 100000
        //socketTimeoutMillis = 100000
    }
}

var fetchAdaptor: ((remote: MicroModule, request: Request) -> Response?)? = null

val client = OkHttp()

fun NativeMicroModule.nativeFetch(request: Request): Response {
    return fetchAdaptor?.let { it(this, request) } ?: client(request)
}

fun NativeMicroModule.nativeFetch(url: Uri) = nativeFetch(Request(Method.GET, url))
fun NativeMicroModule.nativeFetch(url: String) = nativeFetch(Request(Method.GET, url))

