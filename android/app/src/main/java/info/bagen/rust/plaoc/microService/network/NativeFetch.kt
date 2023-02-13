package info.bagen.rust.plaoc.microService.network

import info.bagen.libappmgr.network.KtorManager.apiService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking


fun nativeFetch(url: String): HttpResponse {
    var response: HttpResponse
    runBlocking {
         response = apiService.request(url) {
        }
    }
    println("NativeFetch response:$response")
    return response
}

