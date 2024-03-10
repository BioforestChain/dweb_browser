package org.dweb_browser.dwebview

import com.teamdev.jxbrowser.js.JsAccessible
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.trueAlso

class CloseWatcher(val engine: DWebViewEngine) : ICloseWatcher {
  companion object {
    var acc_id by SafeInt(1)
    const val JS_POLYFILL_KIT = "__native_close_watcher_kit__"
  }

  val consuming = mutableSetOf<String>()
  private val mainScope = MainScope()

  private val install = SuspendOnce {
    engine.beforeCreateWindowSignal.listen { event ->
      val consumeToken = event.url
      engine.closeWatcher.apply {
        if (consuming.remove(consumeToken)) {
          event.consume()
          resolveToken(consumeToken, applyWatcher(event.isUserGesture))
        }
      }
    }
  }

  private val openLock = Mutex()

  init {
    engine.addJavascriptInterface(
      object {
        /**
         * js 创建 CloseWatcher
         */
        @JsAccessible
        fun registryToken(consumeToken: String?) {// 这里用 String? 是为了避免 js 传输错误参数，理论上应该用 Any?
          if (consumeToken.isNullOrBlank()) {
            throw Exception("CloseWatcher.registryToken invalid arguments");
          }
          consuming.add(consumeToken)
          mainScope.launch {
            openLock.withLock {
              install()
              engine.evaluateAsyncJavascriptCode("open('$consumeToken')")
              delay(60) // 经过测试，两次open至少需要50ms才能正确执行，所以这里用一个稍微大一点的数字来确保正确性
            }
          }
        }

        /**
         * js主动关闭 CloseWatcher
         */
        @JsAccessible
        fun tryClose(id: String?) = // 这里用 String? 是为了避免 js 传输错误参数，理论上应该用 Any?
          watchers.find { watcher -> watcher.id == id }?.also {
            mainScope.launch { close(it) }
          }
      }, JS_POLYFILL_KIT
    )
  }

  private val watchers = mutableListOf<ICloseWatcher.IWatcher>()

  inner class Watcher : ICloseWatcher.IWatcher {
    override val id = (acc_id++).toString()
    private var _destroy = atomic(false)
    private val closeMutex = Mutex()
    override suspend fun tryClose(): Boolean = closeMutex.withLock {
      if (_destroy.value) {
        return false
      }
      val defaultPrevented = false;
      /// 尝试去触发客户端的监听，如果客户端有监听的话
      engine.mainFrame.executeJavaScript("$JS_POLYFILL_KIT._watchers?.get('$id')?.dispatchEvent(new CloseEvent('close'));") {}

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
        tryEmitCanCloseMutableFlow()
      }
    }
    return watchers.last()
  }

  fun resolveToken(consumeToken: String, watcher: ICloseWatcher.IWatcher) {
    engine.mainFrame.executeJavaScript(
      "$JS_POLYFILL_KIT._tasks?.get('$consumeToken')('${watcher.id}');"
    ) {};
  }

  /**
   * 现在是否有 CloseWatcher 在等待被关闭
   */
  override val canClose get() = watchers.isNotEmpty()

  private val canCloseMutableFlow = MutableStateFlow(canClose)
  private fun tryEmitCanCloseMutableFlow() {
    engine.ioScope.launch {
      canCloseMutableFlow.emit(canClose)
    }
  }

  override val canCloseFlow by lazy { canCloseMutableFlow.asStateFlow() }

  /**
   * 关闭指定的 CloseWatcher
   */
  override suspend fun close(watcher: ICloseWatcher.IWatcher): Boolean {
    if (watcher.tryClose()) {
      return watchers.remove(watcher).trueAlso {
        tryEmitCanCloseMutableFlow()
      }
    }
    return false
  }

  override suspend fun close() = close(watchers.last())
}