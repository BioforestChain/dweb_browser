package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.teamdev.jxbrowser.js.JsException
import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.dwebProxyService
import org.dweb_browser.core.http.waitReady
import org.dweb_browser.core.ipc.helper.IWebMessageChannel
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.dwebview.engine.window
import org.dweb_browser.dwebview.messagePort.DWebMessageChannel
import org.dweb_browser.dwebview.messagePort.DWebMessagePort
import org.dweb_browser.dwebview.polyfill.DwebViewDesktopPolyfill
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.globalIoScope
import org.dweb_browser.helper.platform.IPureViewBox

actual suspend fun IDWebView.Companion.create(
  mm: MicroModule.Runtime, options: DWebViewOptions, viewBox: IPureViewBox?,
): IDWebView {
  DWebView.prepare()
  return DWebView(DWebViewEngine(mm, options))
}

internal fun IDWebView.Companion.create(engine: DWebViewEngine, initUrl: String?) =
  DWebView(engine, initUrl)

class DWebView(
  val viewEngine: DWebViewEngine, initUrl: String? = null,
) : IDWebView(initUrl ?: viewEngine.options.url) {
  companion object {
    suspend fun prepare() {
      coroutineScope {
        launch {
          DwebViewDesktopPolyfill.prepare();
        }
        dwebProxyService.waitReady()
      }
    }

    init {
      globalIoScope.launch {
        prepare()
      }
    }
  }

  override val remoteMM get() = viewEngine.remoteMM
  override val lifecycleScope: CoroutineScope
    get() = viewEngine.lifecycleScope

  override suspend fun startLoadUrl(url: String): String {
    viewEngine.loadUrl(url)
    return viewEngine.getOriginalUrl()
  }

  override suspend fun resolveUrl(url: String): String = viewEngine.resolveUrl(url)

  override suspend fun getOriginalUrl(): String {
    return viewEngine.getOriginalUrl()
  }

  override suspend fun getTitle(): String {
    return viewEngine.getTitle()
  }

  override suspend fun getIcon() = viewEngine.dwebFavicon.urlFlow.value

  override suspend fun destroy() {
    viewEngine.destroy()
  }

  override suspend fun historyCanGoBack(): Boolean = viewEngine.canGoBack()

  override suspend fun historyGoBack(): Boolean = viewEngine.goBack()

  override suspend fun historyCanGoForward(): Boolean = viewEngine.canGoForward()

  override suspend fun historyGoForward(): Boolean = viewEngine.goForward()

  override suspend fun createMessageChannel(): IWebMessageChannel {
    val channel =
      viewEngine.mainFrame.executeJavaScript<JsObject>("new MessageChannel()") ?: throw JsException(
        "MessageChannel create failed!"
      )
    val port1 = DWebMessagePort(channel.property<JsObject>("port1").get(), this)
    val port2 = DWebMessagePort(channel.property<JsObject>("port2").get(), this)

    channel.close()
    return DWebMessageChannel(port1, port2)
  }

  override suspend fun postMessage(data: String, ports: List<IWebMessagePort>) {
    viewEngine.mainFrame.window().postMessage(data, ports.filterIsInstance<DWebMessagePort>())
  }

  override suspend fun postMessage(data: ByteArray, ports: List<IWebMessagePort>) {
    viewEngine.mainFrame.window().postMessage(data, ports.filterIsInstance<DWebMessagePort>())
  }

//  private var contentScale = 1f
//  override suspend fun setContentScale(scale: Float, width: Float, height: Float, density: Float) {
//    if (contentScale != scale) {
//      contentScale = scale
//      effectEngineScale()
//    }
//  }

  private var renderScale = 1f

  @Composable
  override fun ScaleEffect(scale: Float, modifier: Modifier) {
    if (renderScale != scale) {
      renderScale = scale
      viewEngine.setContentScale(scale.toDouble())
    }
  }

  override suspend fun setPrefersColorScheme(colorScheme: WebColorScheme) {
    WARNING("Not yet implemented setPrefersColorScheme. request [jxBrowser 8.0](https://teamdev.com/jxbrowser/zh/roadmap/#jxbrowser-8-0-0)")
  }

  override suspend fun setVerticalScrollBarVisible(visible: Boolean) {
    viewEngine.setVerticalScrollBarVisible(visible);
  }

  override suspend fun setHorizontalScrollBarVisible(visible: Boolean) {
    viewEngine.setHorizontalScrollBarVisible(visible);
  }

  private val backgroundColorState by lazy { mutableStateOf(Color.Transparent) }
  override var backgroundColor: Color
    get() = backgroundColorState.value
    set(value) {
      backgroundColorState.value = value
    }

  override suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: suspend () -> Unit,
  ): String {
    return viewEngine.evaluateAsyncJavascriptCode(script, afterEval)
  }

  override suspend fun setSafeAreaInset(bounds: PureBounds) {
    // 不需要实现该功能，chromium还没放出桌面端的safeArea，只有Safari的WKWebView需要设置这个
  }

  private val _engineLazy = RememberLazy(viewEngine) { viewEngine }

  override val onDestroy by _engineLazy.then { viewEngine.destroyStateSignal.onDestroy }
  override val loadStateFlow by _engineLazy.then { viewEngine.loadStateFlow.asStateFlow() }
  override val titleFlow by _engineLazy.then { viewEngine.titleFlow }
  override val iconFlow by _engineLazy.then { viewEngine.dwebFavicon.urlFlow }
  override val iconBitmapFlow by _engineLazy.then { viewEngine.iconBitmapFlow }
  override val onBeforeUnload by _engineLazy.then { viewEngine.beforeUnloadSignal.toListener() }
  override val loadingProgressFlow by _engineLazy.then { viewEngine.loadingProgressStateFlow.asStateFlow() }
  override val closeWatcherLazy: RememberLazy<ICloseWatcher> = _engineLazy.then {
    viewEngine.closeWatcher
  }
  override val onCreateWindow by _engineLazy.then { viewEngine.createWindowSignal.toListener() }
  override val overrideUrlLoadingHooks by _engineLazy.then { viewEngine.overrideUrlLoadingHooks }
  override val onDownloadListener by _engineLazy.then { viewEngine.downloadSignal.toListener() }
  override val onScroll by _engineLazy.then { viewEngine.scrollSignal.toListener() }
  override suspend fun openDevTool() {
    viewEngine.browser.devTools().show()
  }

  override suspend fun getIconBitmap() = viewEngine.getFavoriteIcon()

  override suspend fun requestClose() {
    viewEngine.requestClose()
  }

  override fun requestRedraw() {
    WARNING("Not yet implemented requestRefresh")
  }

  init {
    afterInit()
  }
}

fun IDWebView.asDesktop(): DWebView {
  require(this is DWebView)
  return this
}

internal actual fun IDWebView.Companion.supportProfile(): Boolean {
  return true
}