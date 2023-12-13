package org.dweb_browser.dwebview

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.atomicfu.updateAndGet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.dwebHttpGatewayServer
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Bounds
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import reverse_proxy.VoidCallback

val debugDWebView = Debugger("dwebview")

expect suspend fun IDWebView.Companion.create(
  mm: MicroModule, options: DWebViewOptions = DWebViewOptions()
): IDWebView

abstract class IDWebView(initUrl: String?) {
  abstract val scope: CoroutineScope

  @Serializable
  data class UserAgentBrandData(val brand: String, val version: String)

  @OptIn(DelicateCoroutinesApi::class)
  companion object {
    val brands = mutableListOf<UserAgentBrandData>()
    private val proxyAddress = CompletableDeferred<String>()
    suspend fun getProxyAddress() = proxyAddress.await()

    init {
      GlobalScope.launch(ioAsyncExceptionHandler) {
        debugDWebView("reverse_proxy", "starting")
        val backendServerPort = dwebHttpGatewayServer.startServer().toUShort()

        val proxyReadyCallback = object : VoidCallback {
          override fun callback(proxyPort: UShort, frontendPort: UShort) {
            debugDWebView(
              "reverse_proxy",
              "running proxyServerPort=${proxyPort}, frontendServerPort=${frontendPort}, backendServerPort=${backendServerPort}"
            )
            proxyAddress.complete("http://127.0.0.1:${proxyPort}")
          }
        }
        reverse_proxy.start(backendServerPort, proxyReadyCallback)
        debugDWebView("reverse_proxy", "stopped")
      }
    }
  }

  /**
   * 输入要加载的url，返回即将加载的url（url可能会重定向或者重写）
   */
  internal abstract suspend fun startLoadUrl(url: String): String
  internal abstract suspend fun startGoBack(): Boolean

  private val loadUrlTask =
    atomic(LoadUrlTask(if (initUrl.isNullOrEmpty()) "about:blank" else initUrl).apply {
      deferred.complete(url)
    })

  suspend fun loadUrl(url: String, force: Boolean = false): String {
    val newTask = LoadUrlTask(url)

    val curTask = loadUrlTask.updateAndGet { preTask ->
      if (!force && preTask.url == url) {
        preTask
      } else {
        preTask.deferred.cancel(CancellationException("load new url: $url"));
        newTask
      }
    }
    if (curTask == newTask) {
      newTask.startTask(this)
    }
    return curTask.deferred.await()
  }

  /**
   * 出发GoBack也需要修改loadUrlTask的url值
   */
  suspend fun goBack() {
    val dwebView = this
    dwebView.scope.launch {
      dwebView.onReady {
        val newTask = LoadUrlTask(it)
        loadUrlTask.updateAndGet { preTask ->
          if (preTask.url == it) {
            preTask
          } else {
            preTask.deferred.cancel(CancellationException("load new url: $it"));
            newTask
          }
        }
        offListener()
      }
    }
    startGoBack()
  }

  abstract suspend fun resolveUrl(url: String): String

  fun getUrl() = loadUrlTask.value.url
  fun hasUrl() = loadUrlTask.value.url.isBlank()
  abstract suspend fun getTitle(): String
  abstract suspend fun getIcon(): String
  abstract suspend fun destroy()
  abstract suspend fun canGoBack(): Boolean
  abstract suspend fun canGoForward(): Boolean
  //abstract suspend fun goBack(): Boolean
  abstract suspend fun goForward(): Boolean

  abstract suspend fun createMessageChannel(): IWebMessageChannel
  abstract suspend fun postMessage(data: String, ports: List<IWebMessagePort>)
  abstract suspend fun postMessage(data: ByteArray, ports: List<IWebMessagePort>)

  abstract suspend fun setContentScale(scale: Float, width: Float, height: Float, density: Float)
  abstract suspend fun setPrefersColorScheme(colorScheme: WebColorScheme)
  abstract suspend fun setVerticalScrollBarVisible(visible: Boolean)
  abstract suspend fun setHorizontalScrollBarVisible(visible: Boolean)
//  abstract suspend fun setSafeArea(top: Float, left: Float, bottom: Float, right: Float)

  abstract suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: suspend () -> Unit = {}
  ): String

  abstract suspend fun setSafeAreaInset(bounds: Bounds)

  abstract val onDestroy: Signal.Listener<Unit>
  abstract val onLoadStateChange: Signal.Listener<WebLoadState>
  abstract val onReady: Signal.Listener<String>
  abstract val onBeforeUnload: Signal.Listener<WebBeforeUnloadArgs>
  abstract val loadingProgressFlow: SharedFlow<Float>
  abstract val closeWatcher: ICloseWatcher
  abstract val onCreateWindow: Signal.Listener<IDWebView>

  /**
   * 响应webview的事件
   * @param IDWebView
   * @param MotionEventAction 手势移动，按下，抬起等动作
   */
  abstract fun setOnTouchListener(onTouch: (IDWebView, MotionEventAction) -> Boolean)

  /**
   * onScrollChange表示滚动的信息
   * @param IDWebView
   * @param Int scrollX
   * @param Int scrollY
   * @param Int oldScrollX
   * @param Int oldScrollY
   */
  abstract fun setOnScrollChangeListener(onScrollChange: (IDWebView, Int, Int, Int, Int) -> Unit)

  /**
   * 获取webview返回到favorite icon
   */
  abstract suspend fun getFavoriteIcon(): ImageBitmap?
}

class WebBeforeUnloadArgs(
  val message: String,
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

internal data class LoadUrlTask(val url: String) {
  val deferred = CompletableDeferred<String>()
  fun startTask(dwebView: IDWebView) {
    dwebView.scope.launch {
      val loadingUrl = dwebView.startLoadUrl(url)
      dwebView.onLoadStateChange {
        debugDWebView("lin.huang", "onLoadStateChange enter. $it")
        when (it) {
          is WebLoadErrorState -> {
            deferred.completeExceptionally(Exception(it.errorMessage))
            offListener()
          }

          is WebLoadStartState -> {
            if (it.url != loadingUrl) {
              deferred.cancel(CancellationException("start load url: ${it.url}"))
            }
          }

          is WebLoadSuccessState -> {
            deferred.complete(it.url)
            offListener()
          }
        }
      }
    }
  }
}

enum class WebColorScheme {
  Normal, Dark, Light,
}

typealias AsyncChannel = Channel<Result<String>>

// 触发按键的事件
enum class MotionEventAction {
  ACTION_DOWN, ACTION_UP, ACTION_MOVE
}