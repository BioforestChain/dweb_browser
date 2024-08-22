package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.dwebProxyService
import org.dweb_browser.core.http.waitReady
import org.dweb_browser.core.ipc.helper.IWebMessagePort
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.dwebview.messagePort.DWebMessageChannel
import org.dweb_browser.dwebview.messagePort.DWebMessagePort
import org.dweb_browser.dwebview.messagePort.DWebViewWebMessage
import org.dweb_browser.dwebview.polyfill.DwebViewIosPolyfill
import org.dweb_browser.helper.PureBounds
import org.dweb_browser.helper.RememberLazy
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.compose.toComposeColor
import org.dweb_browser.helper.compose.toUIColor
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.setScale
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSArray
import platform.Foundation.NSString
import platform.Foundation.create
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi

@OptIn(ExperimentalForeignApi::class)
actual suspend fun IDWebView.Companion.create(
  mm: MicroModule.Runtime,
  options: DWebViewOptions,
  viewBox: IPureViewBox?,
): IDWebView =
  create(
    CGRectMake(0.0, 0.0, 100.0, 100.0),
    mm,
    options,
    withMainContext { WKWebViewConfiguration() })

@OptIn(ExperimentalForeignApi::class)
suspend fun IDWebView.Companion.create(
  frame: CValue<CGRect>,
  remoteMM: MicroModule.Runtime,
  options: DWebViewOptions = DWebViewOptions(),
  configuration: WKWebViewConfiguration,
) = withMainContext {
  DWebView.prepare()
  create(DWebViewEngine(frame, remoteMM, options, configuration), options.url)
}

@OptIn(ExperimentalForeignApi::class)
internal suspend fun IDWebView.Companion.create(
  engine: DWebViewEngine,
  initUrl: String? = null,
) = DWebView.create(engine, initUrl)

@OptIn(ExperimentalForeignApi::class, NativeRuntimeApi::class)
class DWebView private constructor(
  viewEngine: DWebViewEngine,
  initUrl: String? = null,
) : IDWebView(initUrl ?: viewEngine.options.url) {
  companion object {
    val prepare = SuspendOnce {
      coroutineScope {
        launch { DwebViewIosPolyfill.prepare(); }
        dwebProxyService.waitReady();
      }
    }

    init {
      globalDefaultScope.launch {
        prepare()
      }
    }

    suspend fun create(
      viewEngine: DWebViewEngine,
      initUrl: String? = null,
    ) = DWebView(viewEngine, initUrl).also { dwebView ->
      viewEngine.remoteMM.onBeforeShutdown {
        dwebView.destroy()
      }
    }
  }


  override val remoteMM get() = engine.remoteMM
  private var _engine: DWebViewEngine? = viewEngine
  internal val engine get() = _engine ?: throw NullPointerException("dwebview already been destroy")
  private val _engineLazy = RememberLazy(viewEngine) { _engine }
  override val lifecycleScope get() = engine.lifecycleScope

  val viewWrapper by lazy {
    val wrapper = UIView(engine.frame)
    wrapper.addSubview(engine)
    engine.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
    wrapper
  }

  override suspend fun startLoadUrl(url: String) = withMainContext {
    engine.loadUrl(url)
  }

  override suspend fun historyGoBack(): Boolean = withMainContext {
    engine.canGoBack.trueAlso {
      engine.goBack()
    }
  }

  override suspend fun resolveUrl(url: String) = engine.resolveUrl(url)


  override suspend fun getTitle(): String {
    return engine.title ?: ""
  }


  override suspend fun getOriginalUrl() = withMainContext {
    evaluateAsyncJavascriptCode("window.location.href")
  }

  override suspend fun getIcon() = engine.getFavicon()

  private val destroyStateSignal = DestroyStateSignal(lifecycleScope)

  override val onDestroy = destroyStateSignal.onDestroy
  override val loadStateFlow by _engineLazy.then {
    engine.loadStateFlow.asStateFlow()
  }
  override val onBeforeUnload by _engineLazy.then {
    engine.beforeUnloadSignal.toListener()
  }
  override val loadingProgressFlow by _engineLazy.then {
    engine.loadingProgressStateFlow.asStateFlow()
  }
  override val closeWatcherLazy: RememberLazy<ICloseWatcher> = _engineLazy.then {
    engine.closeWatcher
  }
  override val onCreateWindow by _engineLazy.then {
    engine.createWindowSignal.toListener()
  }
  override val overrideUrlLoadingHooks by _engineLazy.then {
    engine.overrideUrlLoadingHooks
  }
  override val onDownloadListener by _engineLazy.then { engine.downloadSignal.toListener() }
  override val titleFlow by _engineLazy.then { engine.titleObserver.titleFlow }
  override val iconFlow by _engineLazy.then { engine.iconFlow }
  override val iconBitmapFlow by _engineLazy.then { engine.iconBitmapFlow }

  override suspend fun destroy() {
    doDestroy()
  }

  private suspend inline fun doDestroy() {
    if (destroyStateSignal.doDestroy()) {
      withMainContext {
        engine.destroy()
        _engine = null
        /// 开始释放lazy属性绑定
        _engineLazy.setKey(null)
        delay(1000)
        GC.collect()
      }
    }
  }

  override suspend fun historyCanGoBack() = withMainContext { engine.canGoBack }

  override suspend fun historyCanGoForward() = withMainContext { engine.canGoForward }


  override suspend fun historyGoForward() = withMainContext {
    engine.canGoForward.trueAlso {
      engine.goForward()
    }
  }

  override suspend fun createMessageChannel() = withMainContext {
    val deferred = engine.evalAsyncJavascript<NSArray>(
      "nativeCreateMessageChannel()", null,
      DWebViewWebMessage.webMessagePortContentWorld
    )
    val ports_id = deferred.await()
    val port1_id = ports_id.objectAtIndex(0u) as Double
    val port2_id = ports_id.objectAtIndex(1u) as Double

    val port1 = DWebMessagePort(port1_id.toInt(), this, lifecycleScope)
    val port2 = DWebMessagePort(port2_id.toInt(), this, lifecycleScope)

    DWebMessageChannel(port1, port2)
  }

  private var renderScale = 1f

  @Composable
  override fun ScaleEffect(scale: Float, modifier: Modifier) {
    if (renderScale != scale) {
      renderScale = scale
      engine.setScale(scale)
    }
  }

  override suspend fun setPrefersColorScheme(colorScheme: WebColorScheme) {
    engine.overrideUserInterfaceStyle = when (colorScheme) {
      WebColorScheme.Dark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
      WebColorScheme.Light -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
      WebColorScheme.Normal -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
    }
  }

  override suspend fun setVerticalScrollBarVisible(visible: Boolean) = withMainContext {
    engine.scrollView.showsVerticalScrollIndicator = visible
  }

  override suspend fun setHorizontalScrollBarVisible(visible: Boolean) = withMainContext {
    engine.scrollView.showsHorizontalScrollIndicator = visible
  }

  override var backgroundColor: Color
    get() = engine.backgroundColor?.toComposeColor() ?: Color.Transparent
    set(value) {
      engine.backgroundColor = value.toUIColor()
    }

  override suspend fun evaluateAsyncJavascriptCode(
    script: String,
    afterEval: suspend () -> Unit,
  ) = withMainContext {
    engine.awaitAsyncJavaScript<String>(
      "return JSON.stringify(await($script))??'undefined'",
      afterEval = afterEval
    )
  }

  @OptIn(BetaInteropApi::class)
  override suspend fun postMessage(data: String, ports: List<IWebMessagePort>) {
    val portIdList = ports.map {
      require(it is DWebMessagePort)
      it.portId
    }
    withMainContext {
      val arguments = mutableMapOf<NSString, NSObject>().apply {
        put(NSString.create(string = "data"), NSString.create(string = data))
        put(NSString.create(string = "ports"), NSArray.create(portIdList))
      }
      engine.awaitAsyncJavaScript<Unit>(
        "nativeWindowPostMessage(data,ports)",
        arguments.toMap(),
        null,
        DWebViewWebMessage.webMessagePortContentWorld
      )
    }
  }

  @OptIn(BetaInteropApi::class)
  override suspend fun postMessage(data: ByteArray, ports: List<IWebMessagePort>) {
    val portIdList = ports.map {
      require(it is DWebMessagePort)
      it.portId
    }
    withMainContext {
      val arguments = mutableMapOf<NSString, NSObject>().apply {
        put(NSString.create(string = "data"), NSString.create(string = data.decodeToString()))
        put(NSString.create(string = "ports"), NSArray.create(portIdList))
      }
      engine.awaitAsyncJavaScript<Unit>(
        "nativeWindowPostMessage(data,ports)",
        arguments.toMap(),
        null,
        DWebViewWebMessage.webMessagePortContentWorld
      )
    }
  }

  override val onScroll by _engineLazy.then {
    WARNING("Not yet implemented dwebview onScroll")
    Signal<ScrollChangeEvent>().toListener()
  }

  override suspend fun openDevTool() {
    evaluateAsyncJavascriptCode("console.log('openDevTool')")
  }

  override suspend fun getIconBitmap(): ImageBitmap? = engine.iconBitmapFlow.value

  override suspend fun setSafeAreaInset(bounds: PureBounds) {
    engine.safeArea = bounds
  }

  override suspend fun requestClose() {
    val destroyUrl = "about:blank#${randomUUID()}"
    if (loadUrl(destroyUrl) == destroyUrl) {
      doDestroy()
    }
  }

  override fun requestRedraw() {
    WARNING("Not yet implemented requestRefresh")
  }

  init {
    afterInit()
  }
}

/**
 * 该接口请谨慎使用，很容易引起内存泄露
 */
fun IDWebView.asIosWebView(): DWebViewEngine {
  require(this is DWebView)
  return engine
}

fun IDWebView.asIosUIView(): UIView {
  require(this is DWebView)
  return viewWrapper
}

internal actual fun IDWebView.Companion.supportProfile(): Boolean {
  return true
}