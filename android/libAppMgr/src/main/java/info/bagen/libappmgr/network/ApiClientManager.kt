package info.bagen.libappmgr.network

import org.http4k.client.OkHttp
import org.http4k.core.*

class HttpClient {
  private val mClientMemory = OkHttp()
  private val mClientStream = OkHttp(bodyMode = BodyMode.Stream)

  companion object {
    private const val RootPath = "https://shop.plaoc.com/"

    fun getAbsoluteUrl(path: String): String {
      return if (path.startsWith("http")) {
        path
      } else {
        return "$RootPath$path"
      }
    }
  }

  fun requestPath(
    path: String,
    method: Method = Method.GET,
    bodyMode: BodyMode = BodyMode.Memory,
    customRequest: (String, Method) -> Request = defaultRequest
  ): Response {
    return when (bodyMode) {

      BodyMode.Stream -> mClientStream(customRequest(getAbsoluteUrl(path), method))
      else -> mClientMemory(customRequest(getAbsoluteUrl(path), method))
    }
  }

  private val defaultRequest: (String, Method) -> Request = { url, method ->
    Request(method, url) }
}