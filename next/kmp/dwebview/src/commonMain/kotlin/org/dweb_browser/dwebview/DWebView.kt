package org.dweb_browser.dwebview

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.core.ipc.helper.IWebMessageChannel
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Bounds
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal

val debugDWebView = Debugger("dwebview")

expect suspend fun IDWebView.Companion.create(
  mm: MicroModule, options: DWebViewOptions = DWebViewOptions()
): IDWebView

abstract class IDWebView(initUrl: String?) {
  abstract val ioScope: CoroutineScope

  @Serializable
  data class UserAgentBrandData(val brand: String, val version: String)

  @OptIn(DelicateCoroutinesApi::class)
  companion object {
    val brands = mutableListOf<UserAgentBrandData>()
  }

  /**
   * 输入要加载的url，返回即将加载的url（url可能会重定向或者重写）
   */
  internal abstract suspend fun startLoadUrl(url: String): String

  private val urlState by lazy {
    UrlState(this@IDWebView, if (initUrl.isNullOrEmpty()) "about:blank" else initUrl, true)
  }

  suspend fun loadUrl(url: String, force: Boolean = false): String {
    if (!urlState.isUrlEqual(url) || force) {
      urlState.forceLoadUrl(url)
    }
    while (true) {
      try {
        return urlState.awaitUrl()
      } catch (e: CancellationException) {
        continue
      }
    }
  }

  suspend fun reload() = loadUrl(getUrl(), true)

  suspend fun canGoBack() = closeWatcher.canClose || historyCanGoBack()
  suspend fun goBack() {
    if (closeWatcher.canClose) {
      closeWatcher.close()
    } else {
      this.historyGoBack()
    }
  }

  private val canGoBackStateFlowLazy by lazy {
    closeWatcherLazy.then {
      listOf(closeWatcher.canCloseFlow, urlStateFlow).merge().map { canGoBack() }
        .distinctUntilChanged().stateIn(ioScope, SharingStarted.Eagerly, false)
    }
  }
  val canGoBackStateFlow get() = canGoBackStateFlowLazy.value


  abstract suspend fun resolveUrl(url: String): String

  fun getUrl() = urlState.currentUrl
  fun hasUrl() = urlState.currentUrl.isBlank()
  abstract suspend fun getOriginalUrl(): String

  abstract suspend fun getTitle(): String
  abstract suspend fun getIcon(): String
  abstract suspend fun destroy()
  abstract suspend fun historyCanGoBack(): Boolean
  abstract suspend fun historyGoBack(): Boolean
  abstract suspend fun historyCanGoForward(): Boolean
  abstract suspend fun historyGoForward(): Boolean
  val urlStateFlow: StateFlow<String> get() = urlState.stateFlow


  abstract suspend fun createMessageChannel(): IWebMessageChannel
  abstract suspend fun postMessage(data: String, ports: List<IWebMessagePort>)
  abstract suspend fun postMessage(data: ByteArray, ports: List<IWebMessagePort>)

  abstract suspend fun setContentScale(scale: Float, width: Float, height: Float, density: Float)
  abstract suspend fun setPrefersColorScheme(colorScheme: WebColorScheme)
  abstract suspend fun setVerticalScrollBarVisible(visible: Boolean)
  abstract suspend fun setHorizontalScrollBarVisible(visible: Boolean)
//  abstract suspend fun setSafeArea(top: Float, left: Float, bottom: Float, right: Float)

  /**
   * 执行一段表达式，表达式中允许 await 关键字
   *
   * 注意，它是表达式。如果需要多汗函数体，请手动包裹在一个匿名函数中
   */
  abstract suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: suspend () -> Unit = {}
  ): String

  abstract suspend fun setSafeAreaInset(bounds: Bounds)

  abstract val onDestroy: Signal.Listener<Unit>
  abstract val onLoadStateChange: Signal.Listener<WebLoadState>
  abstract val onReady: Signal.Listener<String>
  abstract val onBeforeUnload: Signal.Listener<WebBeforeUnloadArgs>
  abstract val loadingProgressFlow: SharedFlow<Float>
  internal abstract val closeWatcherLazy: RememberLazy<ICloseWatcher>
  val closeWatcher get() = closeWatcherLazy.value
  abstract val onCreateWindow: Signal.Listener<IDWebView>
  abstract val onDownloadListener: Signal.Listener<WebDownloadArgs>
  abstract val onScroll: Signal.Listener<ScrollChangeEvent>

  /**
   * 打开开发者工具
   */
  abstract suspend fun openDevTool():Unit

  /**
   * 获取webview返回到favorite icon
   */
  abstract suspend fun getFavoriteIcon(): ImageBitmap?

  /**
   * 申请关闭webview，会尝试触发 beforeUnload
   */
  abstract suspend fun requestClose(): Unit

  /**
   * 申请刷新webview
   */
  abstract fun requestRefresh(): Unit
//
//  /**
//   * 让页面进入激活态，从而可以做一些需要 激活状 才能执行的事务
//   */
//  abstract suspend fun requestUserActivation(): Unit
}

class ScrollChangeEvent(val scrollX: Int, val scrollY: Int)

class WebBeforeUnloadArgs(
  val message: String,
  val title: String? = null,
  val leaveActionText: String? = null,
  val stayActionText: String? = null,
  val isReload: Boolean = false
) : SynchronizedObject() {
  private val hookReasons = mutableMapOf<Any, WebBeforeUnloadHook>()
  internal fun hook(reason: Any) = synchronized(this) {
    if (isLocked) throw Exception("fail to hook, event already passed.")
    hookReasons.getOrPut(reason) { WebBeforeUnloadHook(message) }
  }

  private var isLocked = false
  private fun syncLock() = synchronized(this) { isLocked = true }

  private val result = CompletableDeferred<Boolean>()
  internal suspend fun waitHookResults() = syncLock().let {
    var isKeep = false;
    val results = hookReasons.values.map { it.result.await() }
    for (unload in results) {
      if (!unload) {
        isKeep = true;
        break
      }
    }
    val unload = !isKeep
    result.complete(unload)
    unload
  }


  suspend fun isKeep() = !result.await()
  suspend fun isUnload() = result.await()
}

class WebBeforeUnloadHook(val message: String) {
  internal val result = CompletableDeferred<Boolean>()
  fun unloadDocument() = result.complete(true)
  fun keepDocument() = result.complete(false)
}


sealed class WebLoadState {};
class WebLoadStartState(val url: String) : WebLoadState();
class WebLoadSuccessState(val url: String) : WebLoadState();
class WebLoadErrorState(val url: String, val errorMessage: String) : WebLoadState();

fun Signal<WebLoadState>.toReadyListener() =
  createChild({ if (it is WebLoadSuccessState) it else null }, { it.url }).toListener()

enum class WebColorScheme {
  Normal, Dark, Light,
}

typealias AsyncChannel = Channel<Result<String>>

@Serializable
data class WebDownloadArgs(
  val userAgent: String,
  val suggestedFilename: String,
  val mimetype: String,
  val contentLength: Long?,
  val url: String,
)


class DestroyStateSignal(val scope: CoroutineScope) {
  var isDestroyed = false
    private set
  private var _destroySignal = SimpleSignal();
  val onDestroy = _destroySignal.toListener()
  fun doDestroy(): Boolean {
    if (isDestroyed) {
      return false
    }
    debugDWebView("DESTROY")
    isDestroyed = true
    scope.launch {
      _destroySignal.emitAndClear()
      delay(2000)
      coroutineContext.cancel(CancellationException("destroy"))
    }
    return true
  }
}