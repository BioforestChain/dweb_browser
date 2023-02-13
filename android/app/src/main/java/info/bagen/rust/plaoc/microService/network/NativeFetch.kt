package info.bagen.rust.plaoc.microService.network

import info.bagen.libappmgr.network.base.BASE_URL
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import kotlinx.coroutines.runBlocking
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

fun nativeFetch(url: String): HttpResponse {
    var response: HttpResponse
    runBlocking {
         response = apiService.request(url) {
             method = HttpMethod.Get
        }
        println("NativeFetch response:$response")
    }
    return response
}

