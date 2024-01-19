package org.dweb_browser.pure.image.offscreenwebcanvas

import org.dweb_browser.pure.http.HttpPureClient
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureServerRequest
import org.dweb_browser.pure.http.defaultHttpPureClient
import org.dweb_browser.pure.http.fetch

/**
 * 网络请求的代理器，因为web中有各种安全性限制，所以这里使用原生的无限制的网络请求提供一个代理
 */
internal class OffscreenWebCanvasFetchProxy(private val client: HttpPureClient = defaultHttpPureClient) {
  suspend fun proxy(request: PureServerRequest): PureResponse {
    val proxyUrl = request.query("url")
    val hook = hooksMap[proxyUrl]?.last()
    val response =
      hook?.let {
        FetchHookContext(
          PureServerRequest(
            proxyUrl,
            PureMethod.GET,
            PureHeaders(request.headers.toList().removeOriginAndAcceptEncoding()),
            request.body
          ),
        ).it()
      } ?: defaultProxy(proxyUrl, request);

    return response.copy(
      headers = PureHeaders(
        response.headers.toList().removeOriginAndAcceptEncoding()
      ).apply { forceCors() })
  }

  private fun PureHeaders.forceCors() {
    init("Access-Control-Allow-Credentials", "true")
    init("Access-Control-Allow-Origin", "*")
    init("Access-Control-Allow-Headers", "*")
    init("Access-Control-Allow-Methods", "*")
  }

  private fun List<Pair<String, String>>.removeOriginAndAcceptEncoding() = filter { (key) ->
    /// 把访问源头过滤掉，未来甚至可能需要额外加上，避免同源限制，但具体如何去加，跟对方的服务器有关系，很难有标准答案，所以这里索性直接移除了
    !(key == "Referer" || key == "Origin" || key == "Host" ||
        // 把编码头去掉，用ktor自己的编码头
        key == "Accept-Encoding")
  }

  private fun List<Pair<String, String>>.removeCorsAndContentEncoding() = filter { (key) ->
    // 这里过滤掉 访问控制相关的配置，重写成全部开放的模式
    !(key.startsWith("Access-Control-Allow-") ||
        // 跟内容编码与长度有关的，也全部关掉，proxyResponse.bodyAsChannel 的时候，得到的其实是解码过的内容，所以这些内容编码与长度的信息已经不可用了
        key == "Content-Encoding" || key == "Content-Length")
  }


  private suspend fun defaultProxy(proxyUrl: String, request: PureServerRequest): PureResponse {
    val response = client.fetch(
      url = proxyUrl,
      headers = PureHeaders(request.headers.toList().removeOriginAndAcceptEncoding()),
      body = request.body
    );
    return response
  }

  private val hooksMap = mutableMapOf<String, MutableList<FetchHook>>()
  fun setHook(url: String, hook: FetchHook): () -> Unit {
    val hooks = hooksMap.getOrPut(url) { mutableListOf() }
    hooks.add(hook)
    return {
      hooks.remove(hook)
      if (hooks.size == 0) {
        hooksMap.remove(url)
      }
    }
  }
}

data class FetchHookContext(
  val request: PureServerRequest,
)

enum class FetchHookReturn {
  Hooked,
  Base,
}
/**
 * 这里使用异步调函数而不是直接返回FetchResponse，目的是使用异步回调函数来传递生命周期的概念，在调用returnBlock结束后，FetchHook可以对一些引用资源进行释放。
 * 这样就可以一些不必要的内存减少拷贝
 */
typealias FetchHook = suspend FetchHookContext.() -> PureResponse?