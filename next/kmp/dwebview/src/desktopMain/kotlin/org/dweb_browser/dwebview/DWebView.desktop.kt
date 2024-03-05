package org.dweb_browser.dwebview

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.Bounds
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.ioAsyncExceptionHandler

actual suspend fun IDWebView.Companion.create(
  mm: MicroModule,
  options: DWebViewOptions
): IDWebView {
  TODO("Not yet implemented")
}

class DWebView(
  val viewEngine: DWebViewEngine,
  initUrl: String? = null
) : IDWebView(initUrl ?: viewEngine.options.url) {
  companion object {
    val prepare = SuspendOnce {

    }

    init {
      CoroutineScope(ioAsyncExceptionHandler).launch {
        prepare()
      }
    }
  }

  override val ioScope: CoroutineScope
    get() = TODO("Not yet implemented")

  override suspend fun startLoadUrl(url: String): String {
    TODO("Not yet implemented")
  }

  override suspend fun resolveUrl(url: String): String {
    TODO("Not yet implemented")
  }

  override suspend fun getOriginalUrl(): String {
    TODO("Not yet implemented")
  }

  override suspend fun getTitle(): String {
    return viewEngine.mainFrame.executeJavaScript<String>("document.title") ?: ""
  }

  override suspend fun getIcon(): String {
    TODO("Not yet implemented")
  }

  override suspend fun destroy() {
    viewEngine.destroy()
  }

  override suspend fun historyCanGoBack(): Boolean {
    TODO("Not yet implemented")
  }

  override suspend fun historyGoBack(): Boolean {
    TODO("Not yet implemented")
  }

  override suspend fun historyCanGoForward(): Boolean {
    TODO("Not yet implemented")
  }

  override suspend fun historyGoForward(): Boolean {
    TODO("Not yet implemented")
  }

  override val urlStateFlow: StateFlow<String>
    get() = TODO("Not yet implemented")

  override suspend fun createMessageChannel(): IWebMessageChannel {
    TODO("Not yet implemented")
  }

  override suspend fun postMessage(data: String, ports: List<IWebMessagePort>) {
    TODO("Not yet implemented")
  }

  override suspend fun postMessage(data: ByteArray, ports: List<IWebMessagePort>) {
    TODO("Not yet implemented")
  }

  override suspend fun setContentScale(scale: Float, width: Float, height: Float, density: Float) {
    TODO("Not yet implemented")
  }

  override suspend fun setPrefersColorScheme(colorScheme: WebColorScheme) {
    TODO("Not yet implemented")
  }

  override suspend fun setVerticalScrollBarVisible(visible: Boolean) {
    TODO("Not yet implemented")
  }

  override suspend fun setHorizontalScrollBarVisible(visible: Boolean) {
    TODO("Not yet implemented")
  }

  override suspend fun evaluateAsyncJavascriptCode(
    script: String,
    afterEval: suspend () -> Unit
  ): String {
    return viewEngine.evaluateAsyncJavascriptCode(script, afterEval)
  }

  override suspend fun setSafeAreaInset(bounds: Bounds) {
    TODO("Not yet implemented")
  }

  override val onDestroy: Signal.Listener<Unit>
    get() = TODO("Not yet implemented")
  override val onLoadStateChange: Signal.Listener<WebLoadState>
    get() = TODO("Not yet implemented")
  override val onReady: Signal.Listener<String>
    get() = TODO("Not yet implemented")
  override val onBeforeUnload: Signal.Listener<WebBeforeUnloadArgs>
    get() = TODO("Not yet implemented")
  override val loadingProgressFlow: SharedFlow<Float>
    get() = TODO("Not yet implemented")
  override val closeWatcherLazy: RememberLazy<ICloseWatcher>
    get() = TODO("Not yet implemented")
  override val onCreateWindow: Signal.Listener<IDWebView>
    get() = TODO("Not yet implemented")
  override val onDownloadListener: Signal.Listener<WebDownloadArgs>
    get() = TODO("Not yet implemented")

  override fun setOnTouchListener(onTouch: (IDWebView, MotionEventAction) -> Boolean) {
    TODO("Not yet implemented")
  }

  override fun setOnScrollChangeListener(onScrollChange: (IDWebView, Int, Int, Int, Int) -> Unit) {
    TODO("Not yet implemented")
  }

  override suspend fun getFavoriteIcon(): ImageBitmap? {
    TODO("Not yet implemented")
  }
}