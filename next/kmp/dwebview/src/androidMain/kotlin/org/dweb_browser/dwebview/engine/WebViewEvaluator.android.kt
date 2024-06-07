package org.dweb_browser.dwebview.engine

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.AsyncChannel
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.launchWithMain
import org.dweb_browser.helper.withMainContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * 代码执行器
 * 这个类的使用，务必在 Main 线程中
 */
class WebViewEvaluator(
  val webView: WebView,
  val scope: CoroutineScope,
) {
  companion object {
    private var idAcc = AtomicInteger(0)
    const val JS_ASYNC_KIT = "__native_async_callback_kit__"
  }

  private val channelMap = mutableMapOf<Int, AsyncChannel>()

  @SuppressLint("JavascriptInterface")
  private fun initKit() {
    webView.addJavascriptInterface(object {
      @JavascriptInterface
      fun resolve(id: Int, data: String) {
        scope.launch {
          channelMap.remove(id)?.also {
            it.send(Result.success(data))
            it.close()
          }
        }
      }

      @JavascriptInterface
      fun reject(id: Int, reason: String) {
        scope.launch {
          channelMap.remove(id)?.also {
            it.send(Result.failure(Exception(reason)))
            it.close()
          }
        }
      }
    }, JS_ASYNC_KIT)
  }

  init {
    initKit()
  }

  /**
   * 执行同步JS代码
   */
  suspend fun evaluateSyncJavascriptCode(script: String) = withMainContext {
    val po = PromiseOut<String>()
    webView.evaluateJavascript(script) {
      po.resolve(it)
    }
    po.waitPromise()
  }

  /**
   * 执行异步JS代码，需要传入一个表达式
   */
  suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: suspend () -> Unit = {},
  ): String {

    val channel: AsyncChannel = Channel()
    val id = idAcc.getAndAdd(1)
    channelMap[id] = channel
    scope.launchWithMain {
      webView.evaluateJavascript(
        """
            void (async()=>{return ($script)})()
                .then(res=>$JS_ASYNC_KIT.resolve($id,JSON.stringify(res)))
                .catch(err=>$JS_ASYNC_KIT.reject($id,String(err)));
            """.trimMargin()
      ) {
        scope.launch {
          afterEval()
        }
      };

    }
    return channel.receive().getOrThrow()
  }

}