package org.dweb_browser.dwebview

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.dwebview.proxy.DwebViewProxy
import org.dweb_browser.helper.Bounds
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.withMainContext

actual suspend fun IDWebView.Companion.create(
  mm: MicroModule,
  options: DWebViewOptions
): IDWebView = DWebView(DWebViewEngine(mm, options))

class DWebView(
  val viewEngine: DWebViewEngine,
  initUrl: String? = null
) : IDWebView(initUrl ?: viewEngine.options.url) {
  companion object {
    val prepare = SuspendOnce {
      coroutineScope {
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

  override suspend fun startLoadUrl(url: String): String = withMainContext {
    viewEngine.loadUrl(url)
    url
  }

  override suspend fun resolveUrl(url: String): String = viewEngine.resolveUrl(url)

  override suspend fun getOriginalUrl(): String = withMainContext {
    viewEngine.evaluateSyncJavascriptCode("javascript:window.location.href;") ?: viewEngine.getOriginalUrl()
  }

  override suspend fun getTitle(): String {
    return viewEngine.mainFrame.executeJavaScript<String>("document.title") ?: viewEngine.getTitle()
  }

  override suspend fun getIcon() = withMainContext {
    viewEngine.evaluateSyncJavascriptCode(
      """
(function getAndroidIcon(preference_size = 64) {
  const iconLinks = [
    ...document.head.querySelectorAll(`link[rel*="icon"]`).values(),
  ]
    .map((ele) => {
      return {
        ele,
        rel: ele.getAttribute("rel"),
      };
    })
    .filter((link) => {
      return (
        link.rel === "icon" ||
        link.rel === "shortcut icon" ||
        link.rel === "apple-touch-icon" ||
        link.rel === "apple-touch-icon-precomposed"
      );
    })
    .map((link, index) => {
      const sizes = parseInt(link.ele.getAttribute("sizes")) || 32;
      return {
        ...link,
        // 上古时代的图标默认大小是32
        sizes,
        weight: sizes * 100 + index,
      };
    })
    .sort((a, b) => {
      const a_diff = Math.abs(a.sizes - preference_size);
      const b_diff = Math.abs(b.sizes - preference_size);
      /// 和预期大小接近的排前面
      if (a_diff !== b_diff) {
        return a_diff - b_diff;
      }
      /// 权重大的排前面
      return b.weight - a.weight;
    });

  const href =
    (
      iconLinks
        /// 优先不获取 ios 的指定图标
        .filter((link) => {
          return (
            link.rel !== "apple-touch-icon" &&
            link.rel !== "apple-touch-icon-precomposed"
          );
        })[0] ??
      /// 获取标准网页图标
      iconLinks[0]
    )?.ele.href ?? "favicon.ico";

  const iconUrl = new URL(href, document.baseURI);
  return iconUrl.href;
})()
"""
    ) ?: ""
  }

  override suspend fun destroy() {
    viewEngine.destroy()
  }

  override suspend fun historyCanGoBack(): Boolean = viewEngine.canGoBack()

  override suspend fun historyGoBack(): Boolean = viewEngine.goBack()

  override suspend fun historyCanGoForward(): Boolean = viewEngine.canGoForward()

  override suspend fun historyGoForward(): Boolean = viewEngine.goForward()

  override val urlStateFlow by lazy { generateOnUrlChangeFromLoadedUrlCache() }

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

  override suspend fun getFavoriteIcon(): ImageBitmap? = viewEngine.getFavoriteIcon()
}