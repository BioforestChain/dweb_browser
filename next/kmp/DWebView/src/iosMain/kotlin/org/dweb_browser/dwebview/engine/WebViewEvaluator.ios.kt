package org.dweb_browser.dwebview.engine

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.AsyncChannel
import org.dweb_browser.helper.runBlockingCatching

class WebViewEvaluator(
  private val engine: DWebViewEngine
) {
  companion object {
    private val idAcc = atomic(0)
    const val JS_ASYNC_KIT = "__native_async_callback_kit__"
  }
  internal val channelMap = mutableMapOf<Int, AsyncChannel>()

  /**
   * 执行同步JS代码
   */
  suspend fun evaluateSyncJavascriptCode(script: String) = engine.evalAsyncJavascript(script)

  /**
   * 执行异步JS代码，需要传入一个表达式
   */
  suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: suspend () -> Unit = {}
  ): String {

    val channel: AsyncChannel = Channel()
    val id = idAcc.getAndAdd(1)
    channelMap[id] = channel
    engine.mainScope.launch {
      engine.evaluateAsyncJavascriptCode(
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