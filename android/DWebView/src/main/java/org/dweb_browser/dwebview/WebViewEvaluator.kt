package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.dweb_browser.helper.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.atomic.AtomicInteger

typealias AsyncChannel = Channel<Result<String>>;

/**
 * 代码执行器
 * 这个类的使用，务必在 Main 线程中
 */
class WebViewEvaluator(
  val webView: WebView,
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
        GlobalScope.launch {
          channelMap.remove(id)?.also {
            it.send(Result.success(data))
            it.close()
          }
        }
      }

      @JavascriptInterface
      fun reject(id: Int, reason: String) {
        GlobalScope.launch {
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
  suspend fun evaluateSyncJavascriptCode(script: String) = withContext(Dispatchers.Main) {
    val po = PromiseOut<String>()
    webView.evaluateJavascript(script) {
      po.resolve(it)
    }
    po.waitPromise()
  }

  /**
   * 执行异步JS代码，需要传入一个表达式
   */
  @OptIn(DelicateCoroutinesApi::class)
  suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: suspend () -> Unit = {}
  ): String {

    val channel: AsyncChannel = Channel()
    val id = idAcc.getAndAdd(1)
    channelMap[id] = channel
    GlobalScope.launch(mainAsyncExceptionHandler) {
      webView.evaluateJavascript(
        """
            void (async()=>{return ($script)})()
                .then(res=>$JS_ASYNC_KIT.resolve($id,JSON.stringify(res)))
                .catch(err=>$JS_ASYNC_KIT.reject($id,String(err)));
            """.trimMargin()
      ) {
        runBlockingCatching {
          afterEval()
        }.getOrNull()
      };

    }
    return channel.receive().getOrThrow()
  }

}