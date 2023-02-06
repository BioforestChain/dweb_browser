package info.bagen.libappmgr.network

import info.bagen.libappmgr.network.base.BASE_URL
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.gson.*

object KtorManager {

    val apiService = HttpClient(CIO) {
        defaultRequest {
            host = BASE_URL
            //port = BASE_PORT
            url { protocol = URLProtocol.HTTPS }
        }
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
}
