package org.dweb_browser.dwebview.closeWatcher

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.dwebview.IDWebViewEngine
import org.dweb_browser.helper.*

open class DCloseWatcher(open val engine: IDWebViewEngine) {

  companion object {
    val acc_id = atomic(1)
    const val JS_POLYFILL_KIT = "__native_close_watcher_kit__"
  }

  val consuming = mutableSetOf<String>()
  private val mainScope = MainScope()

  val watchers = mutableListOf<Watcher>()

  inner class Watcher {
    val id = acc_id.getAndAdd(1).toString()
    private var _destroy = atomic(false)
    private val closeMutex = Mutex()
    suspend fun tryClose(): Boolean = closeMutex.withLock {
      if (_destroy.value) {
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
      withMainContext {
        engine.evaluateJavascriptSync(
          """
                    $JS_POLYFILL_KIT._watchers?.get("$id")?.dispatchEvent(new CloseEvent('close'));
                    """.trimIndent()
        )
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
    engine.evaluateJavascriptSync(
      """
            $JS_POLYFILL_KIT._tasks?.get("$consumeToken")("${watcher.id}");
            """.trimIndent()
    )
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