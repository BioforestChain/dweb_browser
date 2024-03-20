package org.dweb_browser.dwebview

import com.teamdev.jxbrowser.js.JsObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.dwebview.messagePort.DWebMessageChannel
import org.dweb_browser.dwebview.messagePort.DWebMessagePort
import org.dweb_browser.dwebview.polyfill.DwebViewDesktopPolyfill
import org.dweb_browser.dwebview.proxy.DwebViewProxy
import org.dweb_browser.helper.Bounds
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.ioAsyncExceptionHandler

actual suspend fun IDWebView.Companion.create(
  mm: MicroModule, options: DWebViewOptions
): IDWebView {
  DWebView.prepare()
  return DWebView(DWebViewEngine(mm, options))
}


internal fun IDWebView.Companion.create(engine: DWebViewEngine, initUrl: String?) =
  DWebView(engine, initUrl)

class DWebView(
  val viewEngine: DWebViewEngine, initUrl: String? = null
) : IDWebView(initUrl ?: viewEngine.options.url) {
  companion object {
    val prepare = SuspendOnce {
      coroutineScope {
        launch(ioAsyncExceptionHandler) {
          DwebViewDesktopPolyfill.prepare();
        }
        DwebViewProxy.prepare()
      }
    }

    init {
      CoroutineScope(ioAsyncExceptionHandler).launch {
        prepare()
      }
    }
  }

  override val ioScope: CoroutineScope
    get() = viewEngine.ioScope

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

  override suspend fun getIcon() = viewEngine.dwebFavicon.href

  override suspend fun destroy() {
    viewEngine.destroy()
  }

  override suspend fun historyCanGoBack(): Boolean = viewEngine.canGoBack()

  override suspend fun historyGoBack(): Boolean = viewEngine.goBack()

  override suspend fun historyCanGoForward(): Boolean = viewEngine.canGoForward()

  override suspend fun historyGoForward(): Boolean = viewEngine.goForward()

  override suspend fun createMessageChannel(): IWebMessageChannel {
    val channel = viewEngine.mainFrame.executeJavaScript<JsObject>("new MessageChannel()")
    if (channel != null) {
      val port1 = DWebMessagePort(channel.property<JsObject>("port1").get(), this)
      val port2 = DWebMessagePort(channel.property<JsObject>("port2").get(), this)

      return DWebMessageChannel(port1, port2)
    }

    throw NoSuchFieldException("MessageChannel create failed!")
  }

  override suspend fun postMessage(data: String, ports: List<IWebMessagePort>) {
    viewEngine.mainFrame
    ports.first().onMessage.signal.emit(DWebMessage.DWebMessageString(data, ports))
  }

  override suspend fun postMessage(data: ByteArray, ports: List<IWebMessagePort>) {
    ports.first().onMessage.signal.emit(DWebMessage.DWebMessageBytes(data, ports))
  }

  override suspend fun setContentScale(scale: Float, width: Float, height: Float, density: Float) {
    viewEngine.setContentScale(scale.toDouble())
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

  override suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: suspend () -> Unit
  ): String {
    return viewEngine.evaluateAsyncJavascriptCode(script, afterEval)
  }

  override suspend fun setSafeAreaInset(bounds: Bounds) {
    // 不需要实现该功能，chromium还没放出桌面端的safeArea，只有Safari的WKWebView需要设置这个
  }

  private val _engineLazy = RememberLazy(viewEngine) { viewEngine }

  override val onDestroy by _engineLazy.then { viewEngine.destroyStateSignal.onDestroy }
  override val onLoadStateChange by _engineLazy.then { viewEngine.loadStateChangeSignal.toListener() }
  override val onReady get() = viewEngine.onReady
  override val onBeforeUnload by _engineLazy.then { viewEngine.beforeUnloadSignal.toListener() }
  override val loadingProgressFlow by _engineLazy.then { viewEngine.loadingProgressSharedFlow.asSharedFlow() }
  override val closeWatcherLazy: RememberLazy<ICloseWatcher> = _engineLazy.then {
    viewEngine.closeWatcher
  }
  override val onCreateWindow by _engineLazy.then { viewEngine.createWindowSignal.toListener() }
  override val onDownloadListener by _engineLazy.then { viewEngine.downloadSignal.toListener() }
  override val onScroll by _engineLazy.then { viewEngine.scrollSignal.toListener() }

  override suspend fun getFavoriteIcon() = viewEngine.getFavoriteIcon()

  override suspend fun requestClose() {
    viewEngine.requestClose()
  }

  override fun requestRefresh() {
    WARNING("Not yet implemented requestRefresh")
  }
}