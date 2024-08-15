package org.dweb_browser.dwebview.closeWatcher

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.dwebview.ICloseWatcher
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.dwebview.engine.DWebUIDelegate
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.launchWithMain
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withMainContext

internal class CloseWatcher(val engine: DWebViewEngine) : ICloseWatcher {
  companion object {
    private var acc_id by SafeInt(1)
    const val JS_POLYFILL_KIT = "__native_close_watcher_kit__"
    const val JS_EXPORTS_KEY = "__native_close_watcher_exports__"
  }

  val consuming = mutableSetOf<String>()

  val watchers = mutableListOf<ICloseWatcher.IWatcher>()

  inner class Watcher : ICloseWatcher.IWatcher {
    override val id = (acc_id++).toString()
    private var _destroy = atomic(false)
    private val closeMutex = Mutex()
    override suspend fun tryClose(): Boolean = closeMutex.withLock {
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
                    $JS_EXPORTS_KEY.watchers?.get("$id")?.dispatchEvent(new CloseEvent('close'));
                    """.trimIndent()
        )
      }

      if (!defaultPrevented) {
        return destroy()
      }
      return true
    }

    override fun destroy() = !_destroy.getAndSet(true)
  }

  /**
   * 申请一个 CloseWatcher
   */
  fun applyWatcher(isUserGesture: Boolean): ICloseWatcher.IWatcher {
    if (isUserGesture || watchers.size == 0) {
      watchers.add(Watcher()).trueAlso {
        emitCanClose()
      }
    }
    return watchers.last()
  }

  fun resolveToken(consumeToken: String, watcher: ICloseWatcher.IWatcher) {
    engine.evaluateJavascriptSync(
      """
            $JS_EXPORTS_KEY.tasks?.get("$consumeToken")("${watcher.id}");
            """.trimIndent()
    )
  }

  /**
   * 现在是否有 CloseWatcher 在等待被关闭
   */
  override val canClose get() = watchers.isNotEmpty()

  private val canCloseMutableFlow = MutableStateFlow(canClose)
  private fun emitCanClose() {
    canCloseMutableFlow.value = canClose
  }

  override val canCloseFlow by lazy { canCloseMutableFlow.asStateFlow() }


  /**
   * 关闭指定的 CloseWatcher
   */
  override suspend fun close(watcher: ICloseWatcher.IWatcher): Boolean {
    if (watcher.tryClose()) {
      return watchers.remove(watcher).trueAlso { emitCanClose() }
    }
    return false
  }

  override suspend fun close() = close(watchers.last())

  suspend fun registryToken(consumeToken: String) {
    consuming.add(consumeToken)
    withMainContext {
      engine.evaluateJavascriptSync("open('$consumeToken')")
    }
  }

  fun tryClose(id: String) {
    watchers.find { watcher -> watcher.id == id }?.also {
      engine.mainScope.launch { close(it) }
    }
  }

  fun tryDestroy(id: String) {
    watchers.find { watcher -> watcher.id == id }?.also {
      watchers.remove(it)
      it.destroy()
    }
  }

  override fun reset() {
    debugDWebView("CloseWatcher/reset")
    watchers.clear()
    emitCanClose()
  }
}

fun DWebUIDelegate.hookCloseWatcher() {
  createWebViewHooks.add {
    val url = navigationUrl
    when (url != null && engine.closeWatcher.consuming.remove(url)) {
      true -> {
        val isUserGesture =
          forNavigationAction.targetFrame == null || !forNavigationAction.targetFrame!!.mainFrame
        val watcher = engine.closeWatcher.applyWatcher(isUserGesture)

        engine.mainScope.launchWithMain {
          engine.closeWatcher.resolveToken(url, watcher)
        }
        DWebUIDelegate.CreateWebViewHookPolicyDeny
      }

      else -> DWebUIDelegate.CreateWebViewHookPolicyContinue
    }
  }
}