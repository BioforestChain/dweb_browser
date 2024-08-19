package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.helper.IWebMessageChannel
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.sys.tray.TRAY_ITEM_TYPE
import org.dweb_browser.sys.tray.TrayItem
import org.dweb_browser.sys.tray.ext.registryTray

val debugDWebView = Debugger("dwebview")

expect suspend fun IDWebView.Companion.create(
  mm: MicroModule.Runtime,
  options: DWebViewOptions = DWebViewOptions(),
  viewBox: IPureViewBox? = null,
): IDWebView

expect internal fun IDWebView.Companion.supportProfile(): Boolean

abstract class IDWebView(initUrl: String?) {
  abstract val remoteMM: MicroModule.Runtime
  abstract val lifecycleScope: CoroutineScope

  @Serializable
  data class UserAgentBrandData(
    val brand: String,
    val version: String,
    val fullVersion: String = version,
  )

  @OptIn(DelicateCoroutinesApi::class)
  companion object {
    val brands = mutableListOf<UserAgentBrandData>()

    private var devtoolsMenuTrayId: String? = null
    private val tray_lock = Mutex()
    fun registryDevtoolsTray(
      remoteMM: NativeMicroModule.NativeRuntime,
      devtoolsItemTrayId: String,
      trayTitleFlow: StateFlow<String>,
      openDevTool: suspend () -> Unit,
      onDestroy: (() -> Unit) -> Unit,
    ) {
      remoteMM.scopeLaunch(cancelable = true) {
        IDWebView.tray_lock.withLock {
          if (devtoolsMenuTrayId == null) {
            devtoolsMenuTrayId = remoteMM.registryTray(
              TrayItem(
                title = "devtools", type = TRAY_ITEM_TYPE.Menu
              )
            )
          }
          devtoolsMenuTrayId?.isNotEmpty()?.trueAlso {
            val pathname = "/open-devtools/$devtoolsItemTrayId"
            val addOrUpdate: suspend (String) -> String = { trayTitle ->
              remoteMM.registryTray(
                TrayItem(
                  id = devtoolsItemTrayId,
                  parent = devtoolsMenuTrayId,
                  title = trayTitle,
                  url = "file://${remoteMM.mmid}$pathname",
                )
              )
            }
            val job = trayTitleFlow.collectIn(remoteMM.getRuntimeScope()) {
              addOrUpdate(it)
            }

//            addOrUpdate("${getUrl()} - ${getTitle()}")
//            titleFlow.combine(urlStateFlow) { title, urlState ->
//              "$urlState - $title"
//            }.collectIn(remoteMM.getRuntimeScope()) { trayTitle ->
//              addOrUpdate(trayTitle)
//            }
            val nmm = remoteMM as NativeMicroModule.NativeRuntime
            val router = nmm.routes(pathname bind PureMethod.GET by nmm.defineEmptyResponse {
              openDevTool()
            })
            onDestroy {
              job.cancel()
              nmm.removeRouter(router)
            }
          }
        }
      }
    }

    val isSupportProfile = supportProfile()
    val isEnableProfile get() = envSwitch.isEnabled(ENV_SWITCH_KEY.DWEBVIEW_PROFILE)
  }

  /**
   * 输入要加载的url，返回即将加载的url（url可能会重定向或者重写）
   */
  internal abstract suspend fun startLoadUrl(url: String): String

  private val urlState by lazy {
    UrlState(this@IDWebView)
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
        .distinctUntilChanged().stateIn(lifecycleScope, SharingStarted.Eagerly, false)
    }
  }
  val canGoBackStateFlow get() = canGoBackStateFlowLazy.value


  abstract suspend fun resolveUrl(url: String): String

  fun getUrl() = urlState.currentUrl
  fun hasUrl() = urlState.currentUrl.isBlank()
  abstract suspend fun getOriginalUrl(): String

  abstract suspend fun getTitle(): String
  abstract val titleFlow: StateFlow<String>
  abstract suspend fun getIcon(): String
  abstract val iconFlow: StateFlow<String>

  /**
   * 获取webview返回到favorite icon
   */
  abstract suspend fun getIconBitmap(): ImageBitmap?
  abstract val iconBitmapFlow: StateFlow<ImageBitmap?>
  abstract suspend fun destroy()
  abstract suspend fun historyCanGoBack(): Boolean
  abstract suspend fun historyGoBack(): Boolean
  abstract suspend fun historyCanGoForward(): Boolean
  abstract suspend fun historyGoForward(): Boolean
  val urlStateFlow: StateFlow<String> get() = urlState.stateFlow

  abstract suspend fun createMessageChannel(): IWebMessageChannel
  abstract suspend fun postMessage(data: String, ports: List<IWebMessagePort> = listOf())
  abstract suspend fun postMessage(data: ByteArray, ports: List<IWebMessagePort> = listOf())

  /**
   * 设置渲染缩放，但是Compose函数，必要时在主线程调用
   * 如果有动画需求，需要正确地实时地时候才使用该函数
   *
   * 请不要直接使用该函数，请直接使用 @Composable RenderWithScale 函数
   */
  @Composable
  internal abstract fun ScaleEffect(scale: Float, modifier: Modifier)

  abstract suspend fun setPrefersColorScheme(colorScheme: WebColorScheme)
  abstract suspend fun setVerticalScrollBarVisible(visible: Boolean)
  abstract suspend fun setHorizontalScrollBarVisible(visible: Boolean)
  abstract var backgroundColor: Color
//  abstract suspend fun setSafeArea(top: Float, left: Float, bottom: Float, right: Float)

  /**
   * 执行一段表达式，表达式中允许 await 关键字
   *
   * 注意，它是表达式。如果需要函数体，请手动包裹在一个匿名函数中
   */
  abstract suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: suspend () -> Unit = {},
  ): String

  abstract suspend fun setSafeAreaInset(bounds: PureBounds)

  abstract val onDestroy: Signal.Listener<Unit>
  abstract val loadStateFlow: StateFlow<WebLoadState>
  val onReady
    get() = loadStateFlow.mapNotNull { if (it is WebLoadSuccessState) it.url else null }

  abstract val onBeforeUnload: Signal.Listener<WebBeforeUnloadArgs>
  abstract val loadingProgressFlow: StateFlow<Float>
  internal abstract val closeWatcherLazy: RememberLazy<ICloseWatcher>
  val closeWatcher get() = closeWatcherLazy.value
  abstract val onCreateWindow: Signal.Listener<IDWebView>

  /**
   * 拦截跳转
   */
  abstract val overrideUrlLoadingHooks: MutableList<OverrideUrlLoadingParams.() -> UrlLoadingPolicy>

  abstract val onDownloadListener: Signal.Listener<WebDownloadArgs>
  abstract val onScroll: Signal.Listener<ScrollChangeEvent>

  /**
   * 打开开发者工具
   */
  abstract suspend fun openDevTool(): Unit

  /**
   * 申请关闭webview，会尝试触发 beforeUnload
   */
  abstract suspend fun requestClose(): Unit

  /**
   * 申请webview重新绘制
   */
  abstract fun requestRedraw(): Unit
//
//  /**
//   * 让页面进入激活态，从而可以做一些需要 激活状 才能执行的事务
//   */
//  abstract suspend fun requestUserActivation(): Unit

  private val devtoolsItemTrayId by lazy { randomUUID() }
  protected fun afterInit() {
    (remoteMM.debugMM.isEnable && remoteMM is NativeMicroModule.NativeRuntime).trueAlso {
      lifecycleScope.launch {
        val trayTitleFlow = titleFlow.combine(urlStateFlow) { title, urlState ->
          "$urlState - $title"
        }.stateIn(
          lifecycleScope,
          started = SharingStarted.Eagerly,
          initialValue = "${getUrl()} - ${getTitle()}"
        );
        registryDevtoolsTray(remoteMM as NativeMicroModule.NativeRuntime,
          devtoolsItemTrayId,
          trayTitleFlow,
          openDevTool = ::openDevTool,
          onDestroy = { handler ->
            onDestroy { handler() }
          })
      }
    }
  }
}

class ScrollChangeEvent(val scrollX: Int, val scrollY: Int)

class WebBeforeUnloadArgs(
  val message: String,
  val title: String? = null,
  val leaveActionText: String? = null,
  val stayActionText: String? = null,
  val isReload: Boolean = false,
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

data class OverrideUrlLoadingParams(
  var url: String,
  var isMainFrame: Boolean,
)

enum class UrlLoadingPolicy {
  Allow, Block,
}

sealed class WebLoadState(val url: String) {};
class WebLoadStartState(url: String) : WebLoadState(url)
class WebLoadSuccessState(url: String) : WebLoadState(url)
class WebLoadErrorState(url: String, val errorMessage: String) : WebLoadState(url)

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
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
      _destroySignal.emitAndClear()
    }
    return true
  }
}