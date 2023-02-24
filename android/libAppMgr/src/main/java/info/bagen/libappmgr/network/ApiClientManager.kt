package info.bagen.libappmgr.network

import org.http4k.client.OkHttp
import org.http4k.core.*

class HttpClient {
    private val mClientMemory = OkHttp()
    private val mClientStream = OkHttp(bodyMode = BodyMode.Stream)
    companion object {
        private const val RootPath = "https://shop.plaoc.com/"
    }

    fun requestPath(
        path: String,
        method: Method = Method.GET,
        bodyMode: BodyMode = BodyMode.Stream,
        customRequest: (String, Method) -> Request = defaultRequest
    ): Response {
        return when(bodyMode) {
            BodyMode.Stream -> mClientStream(customRequest(path, method))
            else -> mClientMemory(customRequest(path, method))
        }
    }

    private val defaultRequest: (String, Method) -> Request = { path, method ->
        Request(method, createPath(path))
    }

    private fun createPath(path: String): String {
        return if (path.startsWith("http")) {
            path
        } else {
            return "$RootPath$path"
        }
    }
}