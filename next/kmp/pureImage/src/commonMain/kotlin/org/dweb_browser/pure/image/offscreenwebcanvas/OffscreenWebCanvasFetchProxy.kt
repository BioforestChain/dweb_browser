package org.dweb_browser.pure.image.offscreenwebcanvas

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.ResponseHeaders
import io.ktor.server.response.appendIfAbsent
import io.ktor.server.response.header
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.util.getOrFail
import io.ktor.util.flattenEntries
import io.ktor.utils.io.copyAndClose
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.engine.httpFetcher

/**
 * 网络请求的代理器，因为web中有各种安全性限制，所以这里使用原生的无限制的网络请求提供一个代理
 */
internal class OffscreenWebCanvasFetchProxy(private val client: HttpClient = httpFetcher) {
  suspend fun proxy(call: ApplicationCall) {
    val proxyUrl = call.request.queryParameters.getOrFail("url")
    val hook = hooksMap[proxyUrl]?.last()
    if (hook != null) {
      val hookReturn = FetchHookContext(
        PureClientRequest(
          proxyUrl,
          PureMethod.GET,
          PureHeaders(call.request.headers.flattenEntries().removeOriginAndAcceptEncoding()),
          IPureBody.from(PureStream(call.request.receiveChannel()))
        ),
      ) { res ->
        if (res == null) {
          return@FetchHookContext FetchHookReturn.Base
        }
        for ((key, value) in res.headers.toList().removeCorsAndContentEncoding()) {
          call.response.header(key, value)
        }
        call.response.headers.forceCors()
        call.respondBytesWriter(status = res.status) {
          res.body.toPureStream().getReader("respondBytesWriter").copyAndClose(this)
        }
        FetchHookReturn.Hooked
      }.hook()
      if (hookReturn == FetchHookReturn.Base) {
        defaultProxy(proxyUrl, call)
      }
    } else defaultProxy(proxyUrl, call)
  }

  private fun ResponseHeaders.forceCors() {
    appendIfAbsent("Access-Control-Allow-Credentials", "true")
    appendIfAbsent("Access-Control-Allow-Origin", "*")
    appendIfAbsent("Access-Control-Allow-Headers", "*")
    appendIfAbsent("Access-Control-Allow-Methods", "*")
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


  private suspend fun defaultProxy(proxyUrl: String, call: ApplicationCall) {
    call.request.receiveChannel()
    client.prepareGet(proxyUrl) {
      for ((key, values) in call.request.headers.flattenEntries().removeOriginAndAcceptEncoding()) {
        header(key, values)
      }
    }.execute { proxyResponse ->
      call.response.headers.apply {
        for ((key, value) in proxyResponse.headers.flattenEntries()
          .removeCorsAndContentEncoding()) {
          append(key, value)
        }
        forceCors()
      }
      call.respondBytesWriter(status = proxyResponse.status) {
        proxyResponse.bodyAsChannel().copyAndClose(this)
      }
    }
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

//// TODO 将来使用 PureRequest/PureResponse 替代
//class FetchRequest(
//  val url: String, val headers: List<Pair<String, String>>, val body: ByteReadChannel
//)
//
//class FetchResponse(
//  val status: HttpStatusCode, val headers: List<Pair<String, String>>, val body: ByteReadChannel
//)
//
//fun PureResponse.toFetchResponse() =
//  FetchResponse(status, headers.toList(), body.toPureStream().getReader("toFetchResponse"))

data class FetchHookContext(
  val request: PureClientRequest, val returnResponse: suspend (PureResponse?) -> FetchHookReturn
)

enum class FetchHookReturn {
  Hooked,
  Base,
}
/**
 * 这里使用异步调函数而不是直接返回FetchResponse，目的是使用异步回调函数来传递生命周期的概念，在调用returnBlock结束后，FetchHook可以对一些引用资源进行释放。
 * 这样就可以一些不必要的内存减少拷贝
 */
typealias FetchHook = suspend FetchHookContext.() -> FetchHookReturn