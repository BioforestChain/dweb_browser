package org.dweb_browser.dwebview.closeWatcher

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.dweb_browser.dwebview.base.ViewItem
import org.dweb_browser.helper.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("JavascriptInterface")
class CloseWatcher(val viewItem: ViewItem) {

  companion object {
    val acc_id = AtomicInteger(1)
    const val JS_POLYFILL_KIT = "__native_close_watcher_kit__"
  }

  val consuming = mutableSetOf<String>()
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  init {
    viewItem.webView.addJavascriptInterface(
      object {
        /**
         * js 创建 CloseWatcher
         */
        @JavascriptInterface
        fun registryToken(consumeToken: String) {
          if (consumeToken.isNullOrBlank()) {
            throw Exception("CloseWatcher.registryToken invalid arguments");
          }
          consuming.add(consumeToken)
          ioAsyncScope.launch {
            withContext(Dispatchers.Main) {
              viewItem.webView.evaluateJavascript("open('$consumeToken')", {})
            }
          }
        }

        /**
         * js主动关闭 CloseWatcher
         */
        @JavascriptInterface
        fun tryClose(id: String) =
          watchers.find { watcher -> watcher.id == id }?.also {
            ioAsyncScope.launch { close(it) }
          }
      },
      JS_POLYFILL_KIT
    )
  }

  private val watchers = mutableListOf<Watcher>()

  inner class Watcher {
    val id = acc_id.getAndAdd(1).toString()
    private var _destroy = AtomicBoolean(false)
    private val closeMutex = Mutex()
    suspend fun tryClose(): Boolean = closeMutex.withLock {
      if (_destroy.get()) {
        return false
      }
      val defaultPrevented = false;
      /// 目前，不实现cancel，因为没有合适的API来实现，虽然能模拟，但暂时没必要。等官方接口出来，我们自己的实现尽可能保持接口的简单性
//           viewItem.webView.evaluateAsyncJavascriptCode(
//                """
//                new Promise((resolve,reject)=>{
//                    try{
//                        const watchers = ${JS_POLYFILL_KIT}._watchers;
//                        const watcher = watchers?.get("$id");
//                        if (watcher) {
//                            const event = new CustomEvent('cancel',{cancelable:true})
//                            watcher.dispatchEvent(event);
//                            resolve(event.defaultPrevented)
//                            watchers.delete("$id");
//                        }
//                        resolve(false)
//                    }catch(err){resolve(false)}
//                })
//                """.trimIndent()
//            ) {}.toBoolean();

      /// 尝试去触发客户端的监听，如果客户端有监听的话
      withContext(mainAsyncExceptionHandler) {
        viewItem.webView.evaluateJavascript(
          """
                    $JS_POLYFILL_KIT._watchers?.get("$id")?.dispatchEvent(new CloseEvent('close'));
                    """.trimIndent()
        ) {}
      }

      if (!defaultPrevented) {
        return destroy()
      }
      return true
    }

    fun destroy() = !_destroy.getAndSet(true)
  }

  /**
   * 申请一个 CloseWatcher
   */
  fun apply(isUserGesture: Boolean): Watcher {
    if (isUserGesture || watchers.size == 0) {
      watchers.add(Watcher())
    }
    return watchers.last()
  }

  fun resolveToken(consumeToken: String, watcher: Watcher) {
    viewItem.webView.evaluateJavascript(
      """
            $JS_POLYFILL_KIT._tasks?.get("$consumeToken")("${watcher.id}");
            """.trimIndent()
    ) {};
  }

  /**
   * 现在是否有 CloseWatcher 在等待被关闭
   */
  val canClose get() = watchers.size > 0

  /**
   * 关闭指定的 CloseWatcher
   */
  suspend fun close(watcher: Watcher = watchers.last()): Boolean {
    if (watcher.tryClose()) {
      return watchers.remove(watcher)
    }
    return false
  }
}