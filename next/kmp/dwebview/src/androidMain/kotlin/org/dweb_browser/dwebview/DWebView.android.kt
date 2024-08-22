package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.fastRoundToInt
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.IWebMessageChannel
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DWebMessagePort.Companion.into
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.dwebview.polyfill.DwebViewAndroidPolyfill
import org.dweb_browser.dwebview.proxy.DWebViewProxyOverride
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
        DWebViewProxyOverride.prepare()
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

  private var renderScale = 1f

  /**
   * 以下是官方关于 setInitialScale 介绍：
   * https://developer.android.com/reference/android/webkit/WebView#setInitialScale(int)
   * ```md
   * 1. setInitialScale 方法：该方法用于设置 WebView 的初始缩放级别。你提供的值决定了 WebView 首次加载时的缩放比例。
   * 2. 值为 0：如果将初始缩放值设置为 0，则使用默认的缩放行为。
   * 3. 默认缩放行为：
   *    - 默认的缩放行为依赖于 WebView 的设置：
   *    - useWideViewPort：如果启用了这个设置，WebView 将使用宽视口。
   *    - loadWithOverviewMode：如果启用了这个设置，WebView 将以概览模式加载内容，使内容宽度适应 WebView 的宽度。
   * 4. 缩放行为：
   *    - 如果内容的宽度适合 WebView，则默认缩放级别设置为 100%。
   *    - 对于宽内容，是否进行缩放取决于 loadWithOverviewMode 设置：
   *    - 为真（True）：内容会被缩小以适应 WebView 的宽度。
   *    - 为假（False）：内容不会被缩小，它会保持自然大小。
   * 5. 自定义初始缩放：
   *    - 如果设置一个大于 0 的初始缩放值，WebView 将以该缩放级别开始显示内容。
   * 6. 关于屏幕密度的说明：
   *    - 与 HTML 中的 viewport meta 标签设置不同，setInitialScale 方法不会考虑设备的屏幕密度。
   *
   * 总结来说，setInitialScale 方法允许你控制 WebView 的初始缩放级别。如果使用默认值（0），缩放级别将根据 WebView 的设置和内容宽度处理方式来确定。
   * ```
   *
   * 配合我在代码中注释里对于 settings.useWideViewPort 与 settings.loadWithOverviewMode 的解释。
   * 这里给出如下方案来实现网页缩放需求：
   *
   * 1. 如果scale==100，那么使用标准模式
   * 2. 如果scale!=100，那么使用概览模式
   *    使用概览模式的缺陷在于，用户能强行进行手势对网页进行缩放操作，缩放到标准模式的大小
   *
   * 注意，网页的缩放不可以用 view.scaleX/scaleY，它会导致一些原生控件未知的渲染异常，比方说 text-range 的选择器错位
   */
  @Composable
  override fun ScaleEffect(scale: Float, modifier: Modifier) {
    if (renderScale != scale) {
      renderScale = scale
      if (scale == 1f) {
        engine.settings.useWideViewPort = false
        engine.settings.loadWithOverviewMode = false
        engine.setInitialScale(0)
      } else {
        engine.settings.useWideViewPort = false
        engine.settings.loadWithOverviewMode = true
        val scaleInPercent = (scale * LocalDensity.current.density * 100).fastRoundToInt()
        engine.setInitialScale(scaleInPercent)
      }
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

  private var bgColor = Color.Transparent
  override var backgroundColor: Color
    get() = bgColor
    set(value) {
      if (value != bgColor) {
        bgColor = value
        engine.setBackgroundColor(bgColor.toArgb())
      }
    }

  override suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: suspend () -> Unit,
  ): String = engine.evaluateAsyncJavascriptCode(script, afterEval)

  override val onDestroy by lazy { engine.destroyStateSignal.onDestroy }
  override val titleFlow by lazy { engine.titleFlow }
  override val loadStateFlow by lazy { engine.loadStateFlow.asStateFlow() }

  override val onBeforeUnload by lazy { engine.dWebChromeClient.beforeUnloadSignal.toListener() }
  override val loadingProgressFlow by lazy { engine.dWebChromeClient.loadingProgressStateFlow.asStateFlow() }

  override val closeWatcherLazy = RememberLazy<ICloseWatcher>(engine) {
    engine.closeWatcher
  }
  override val onCreateWindow by lazy { engine.createWindowSignal.toListener() }
  override val overrideUrlLoadingHooks by lazy { engine.overrideUrlLoadingHooks }
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

  override fun requestRedraw() {
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