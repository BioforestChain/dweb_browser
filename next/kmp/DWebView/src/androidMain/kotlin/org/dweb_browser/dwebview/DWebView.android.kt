package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.WebMessage
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
import androidx.webkit.WebSettingsCompat.DARK_STRATEGY_USER_AGENT_DARKENING_ONLY
import androidx.webkit.WebSettingsCompat.FORCE_DARK_AUTO
import androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
import androidx.webkit.WebSettingsCompat.FORCE_DARK_ON
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.flow.asSharedFlow
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.dwebview.DWebMessagePort.Companion.into
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.withMainContext

actual suspend fun IDWebView.Companion.create(
  mm: MicroModule, options: DWebViewOptions
): IDWebView = create(NativeMicroModule.Companion.getAppContext(), mm, options)

suspend fun IDWebView.Companion.create(
  /**
   * 一个WebView的上下文
   */
  context: Context,
  /// 这两个参数是用来实现请求拦截与转发的
  remoteMM: MicroModule,
  /**
   * 一些DWebView自定义的参数
   */
  options: DWebViewOptions = DWebViewOptions(),
  /**
   * 该参数的存在，是用来做一些跟交互式界面相关的行为的，交互式界面需要有一个上下文，比如文件选择、权限申请等行为。
   * 我们将这些功能都写到了BaseActivity上，如果没有提供该对象，则相关的功能将会被禁用
   */
  activity: org.dweb_browser.helper.android.BaseActivity? = null
): IDWebView =
  withMainContext { create(DWebViewEngine(context, remoteMM, options, activity), options.url) }

internal fun IDWebView.Companion.create(engine: DWebViewEngine, initUrl: String?) =
  DWebView(engine, initUrl)

class DWebView(internal val engine: DWebViewEngine, initUrl: String? = null) : IDWebView(initUrl) {
  override val scope get() = engine.ioScope
  override suspend fun startLoadUrl(url: String) = withMainContext {
    engine.loadUrl(url)
    url
  }

  override suspend fun resolveUrl(url: String) = engine.resolveUrl(url)


  override suspend fun getTitle() = withMainContext {
    engine.title ?: ""
  }

  override suspend fun getIcon() = withMainContext {
    engine.evaluateSyncJavascriptCode(
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
    )
  }

  override suspend fun destroy() = withMainContext {
    engine.destroy()
  }

  override suspend fun canGoBack() = withMainContext { engine.canGoBack() }

  override suspend fun canGoForward() = withMainContext { engine.canGoForward() }

  override suspend fun goBack() = withMainContext {
    if (engine.canGoBack()) {
      engine.goBack()
      true// TODO 能否有goBack钩子？
    } else {
      false
    }
  }

  override suspend fun goForward() = withMainContext {
    if (engine.canGoForward()) {
      engine.goForward()
      true// TODO 能否有goForward钩子？
    } else {
      false
    }
  }

  override suspend fun createMessageChannel(): IWebMessageChannel = withMainContext {
    DWebMessageChannel(engine.createWebMessageChannel())
  }

  override suspend fun postMessage(data: String, ports: List<IWebMessagePort>) = withMainContext {
    engine.postWebMessage(WebMessage(data, ports.map { it.into() }.toTypedArray()), Uri.EMPTY)
  }

  override suspend fun setContentScale(scale: Float) = withMainContext {
    engine.setInitialScale((scale * 100).toInt())
  }

  override suspend fun setPrefersColorScheme(colorScheme: WebColorScheme) {
    if (!WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
      return
    }
    when (colorScheme) {
      WebColorScheme.Normal -> {
        WebSettingsCompat.setForceDark(engine.settings, FORCE_DARK_AUTO)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
          WebSettingsCompat.setForceDarkStrategy(
            engine.settings, DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
          )
        }
      }

      WebColorScheme.Dark -> {
        WebSettingsCompat.setForceDark(engine.settings, FORCE_DARK_ON)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
          WebSettingsCompat.setForceDarkStrategy(
            engine.settings, DARK_STRATEGY_USER_AGENT_DARKENING_ONLY
          )
        }
      }

      WebColorScheme.Light -> {
        WebSettingsCompat.setForceDark(engine.settings, FORCE_DARK_OFF)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
          WebSettingsCompat.setForceDarkStrategy(
            engine.settings, DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
          )
        }
      }
    }
  }

  override suspend fun setVerticalScrollBarVisible(visible: Boolean) {
    engine.isVerticalScrollBarEnabled = visible
  }

  override suspend fun setHorizontalScrollBarVisible(visible: Boolean) {
    engine.isHorizontalScrollBarEnabled = visible
  }

  override suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: suspend () -> Unit
  ): String = engine.evaluateAsyncJavascriptCode(script, afterEval)

  override val onDestroy = engine.onDestroy
  override val onLoadStateChange by lazy { engine.dWebViewClient.loadStateChangeSignal.toListener() }
  override val onReady get() = engine.dWebViewClient.onReady

  override val onBeforeUnload by lazy { engine.dWebChromeClient.beforeUnloadSignal.toListener() }
  override val loadingProgressFlow by lazy { engine.dWebChromeClient.loadingProgressSharedFlow.asSharedFlow() }
  override val closeWatcher = engine.closeWatcher
  override val onCreateWindow by lazy { engine.createWindowSignal.toListener() }

  @SuppressLint("ClickableViewAccessibility")
  override fun setOnTouchListener(onTouch: (IDWebView, MotionEventAction) -> Boolean) {
    engine.setOnTouchListener { _, event ->
      val motionEvent = when (event.action) {
        android.view.MotionEvent.ACTION_DOWN -> MotionEventAction.ACTION_DOWN
        android.view.MotionEvent.ACTION_UP -> MotionEventAction.ACTION_UP
        android.view.MotionEvent.ACTION_MOVE -> MotionEventAction.ACTION_MOVE
        else -> return@setOnTouchListener false
      }
      onTouch(this, motionEvent)
    }
  }

  override fun setOnScrollChangeListener(onScrollChange: (IDWebView, Int, Int, Int, Int) -> Unit) {
    engine.setOnScrollChangeListener { _, scrollX, scrollY, oldScrollX, oldScrollY ->
      onScrollChange(this, scrollX, scrollY, oldScrollX, oldScrollY)
    }
  }

  override fun getFavoriteIcon(): ImageBitmap? {
    return this.asAndroidWebView().favicon?.asImageBitmap()
  }
}

//#region 一些针对平台的接口
fun IDWebView.asAndroidWebView(): DWebViewEngine {
  require(this is DWebView)
  return engine
}
//#endregion
