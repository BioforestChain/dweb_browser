package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING
import androidx.webkit.WebSettingsCompat.DARK_STRATEGY_USER_AGENT_DARKENING_ONLY
import androidx.webkit.WebSettingsCompat.FORCE_DARK_AUTO
import androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
import androidx.webkit.WebSettingsCompat.FORCE_DARK_ON
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.IWebMessageChannel
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DWebMessagePort.Companion.into
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.dwebview.polyfill.DwebViewAndroidPolyfill
import org.dweb_browser.dwebview.proxy.DwebViewProxyOverride
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.asAndroid
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.withMainContext

actual suspend fun IDWebView.Companion.create(
  mm: MicroModule.Runtime, options: DWebViewOptions, viewBox: IPureViewBox?,
): IDWebView = create(getAppContextUnsafe(), mm, options, viewBox?.asAndroid()?.activity)

suspend fun IDWebView.Companion.create(
  /**
   * 一个WebView的上下文
   */
  context: Context,
  /// 这两个参数是用来实现请求拦截与转发的
  remoteMM: MicroModule.Runtime,
  /**
   * 一些DWebView自定义的参数
   */
  options: DWebViewOptions = DWebViewOptions(),
  /**
   * 该参数的存在，是用来做一些跟交互式界面相关的行为的，交互式界面需要有一个上下文，比如文件选择、权限申请等行为。
   * 我们将这些功能都写到了BaseActivity上，如果没有提供该对象，则相关的功能将会被禁用
   */
  activity: org.dweb_browser.helper.android.BaseActivity? = null,
): IDWebView = withMainContext {
  DWebView.prepare()
  create(DWebViewEngine(context, remoteMM, options, activity), options.url)
}

internal suspend fun IDWebView.Companion.create(engine: DWebViewEngine, initUrl: String?) =
  DWebView.create(engine, initUrl)

class DWebView private constructor(internal val engine: DWebViewEngine, initUrl: String? = null) :
  IDWebView(initUrl) {
  companion object {
    val prepare = SuspendOnce {
      coroutineScope {
        launch { DwebViewAndroidPolyfill.prepare() }
        DwebViewProxyOverride.prepare()
      }
    }

    init {
      globalDefaultScope.launch {
        prepare()
      }
    }

    suspend fun create(engine: DWebViewEngine, initUrl: String? = null) =
      DWebView(engine, initUrl).also { dwebView ->
        engine.remoteMM.onBeforeShutdown {
          dwebView.destroy()
        }
      }
  }

  override val remoteMM get() = engine.remoteMM
  override val lifecycleScope get() = engine.lifecycleScope
  override suspend fun startLoadUrl(url: String) = withMainContext {
    engine.loadUrl(url)
    url
  }

  override suspend fun resolveUrl(url: String) = engine.resolveUrl(url)
  override suspend fun getOriginalUrl() = withMainContext {
    engine.url ?: "about:blank"
  }


  override suspend fun getTitle() = withMainContext {
    engine.title ?: ""
  }

  override suspend fun getIcon() = engine.dwebFavicon.urlFlow.value
  override suspend fun destroy() = withMainContext {
    engine.destroy()
  }

  override suspend fun historyCanGoBack() = withMainContext { engine.canGoBack() }

  override suspend fun historyGoBack(): Boolean = withMainContext {
    if (engine.canGoBack()) {
      engine.goBack()
      true
    } else {
      false
    }
  }

  override suspend fun historyCanGoForward() = withMainContext { engine.canGoForward() }

  override suspend fun historyGoForward() = withMainContext {
    if (engine.canGoForward()) {
      engine.goForward()
      true// TODO 能否有goForward钩子？
    } else {
      false
    }
  }

  @SuppressLint("RequiresFeature")
  override suspend fun createMessageChannel(): IWebMessageChannel = withMainContext {
    DWebMessageChannel(WebViewCompat.createWebMessageChannel(engine), engine)
  }

  @SuppressLint("RequiresFeature")
  override suspend fun postMessage(data: String, ports: List<IWebMessagePort>) = withMainContext {
    WebViewCompat.postWebMessage(
      engine, WebMessageCompat(data, ports.map { it.into() }.toTypedArray()), Uri.EMPTY
    )
  }

  @SuppressLint("RequiresFeature")
  override suspend fun postMessage(data: ByteArray, ports: List<IWebMessagePort>) =
    withMainContext {
      WebViewCompat.postWebMessage(
        engine, WebMessageCompat(data, ports.map { it.into() }.toTypedArray()), Uri.EMPTY
      )
    }

  private var contentScale = 1f
  override suspend fun setContentScale(scale: Float, width: Float, height: Float, density: Float) =
    withMainContext {
      if (contentScale != scale) {
        contentScale = scale
        effectEngineScale()
      }
    }

  private var renderScale = 1f
  @Composable
  override fun ScaleRender(scale: Float) {
    if (renderScale != scale) {
      renderScale = scale
      effectEngineScale()
    }
  }

  internal val viewScale = mutableFloatStateOf(1f)
  private fun effectEngineScale() {
    (renderScale * contentScale).also { s ->
      viewScale.floatValue = s
      engine.scaleX = s
      engine.scaleY = s
    }
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
    script: String, afterEval: suspend () -> Unit,
  ): String = engine.evaluateAsyncJavascriptCode(script, afterEval)

  override val onDestroy by lazy { engine.destroyStateSignal.onDestroy }
  override val titleFlow by lazy { engine.titleFlow }
  override val onLoadStateChange by lazy { engine.dWebViewClient.loadStateChangeSignal.toListener() }
  override val onReady get() = engine.dWebViewClient.onReady

  override val onBeforeUnload by lazy { engine.dWebChromeClient.beforeUnloadSignal.toListener() }
  override val loadingProgressFlow by lazy { engine.dWebChromeClient.loadingProgressSharedFlow.asSharedFlow() }

  override val closeWatcherLazy = RememberLazy<ICloseWatcher>(engine) {
    engine.closeWatcher
  }
  override val onCreateWindow by lazy { engine.createWindowSignal.toListener() }
  override val onDownloadListener by lazy { engine.dWebDownloadListener.downloadSignal.toListener() }
  override val onScroll by lazy { engine.scrollSignal.toListener() }
  override val iconBitmapFlow by lazy { engine.iconBitmapFlow }
  override val iconFlow by lazy { engine.dwebFavicon.urlFlow }

  override suspend fun getIconBitmap() =
    engine.iconBitmapFlow.value ?: withMainContext { engine.favicon?.asImageBitmap() }

  override suspend fun setSafeAreaInset(bounds: PureBounds) = withMainContext {
    engine.safeArea = bounds
  }

  override suspend fun requestClose() {
    val destroyUrl = "about:blank#${randomUUID()}"
    if (loadUrl(destroyUrl) == destroyUrl) {
      destroy()
    }
  }

  override suspend fun openDevTool() {
    evaluateAsyncJavascriptCode("console.log('openDevTool')")
  }

  override fun requestRefresh() {
    engine.invalidate()
  }

  init {
    afterInit()
  }
}

//#region 一些针对平台的接口
fun IDWebView.asAndroidWebView(): DWebViewEngine {
  require(this is DWebView)
  return engine
}

//#endregion
internal actual fun IDWebView.Companion.supportProfile(): Boolean {
  return WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)
}