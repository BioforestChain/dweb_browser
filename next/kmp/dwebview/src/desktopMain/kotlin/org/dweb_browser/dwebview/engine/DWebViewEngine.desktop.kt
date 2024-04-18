package org.dweb_browser.dwebview.engine

import androidx.compose.ui.graphics.ImageBitmap
import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.browser.CloseOptions
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback
import com.teamdev.jxbrowser.browser.callback.ShowContextMenuCallback
import com.teamdev.jxbrowser.browser.event.BrowserClosed
import com.teamdev.jxbrowser.browser.internal.rpc.ConsoleMessageReceived
import com.teamdev.jxbrowser.dom.event.EventParams
import com.teamdev.jxbrowser.dom.event.EventType
import com.teamdev.jxbrowser.dom.event.MouseEvent
import com.teamdev.jxbrowser.dom.event.MouseEventParams
import com.teamdev.jxbrowser.dom.event.UiEventModifierParams
import com.teamdev.jxbrowser.frame.Frame
import com.teamdev.jxbrowser.js.ConsoleMessageLevel
import com.teamdev.jxbrowser.js.JsException
import com.teamdev.jxbrowser.js.JsPromise
import com.teamdev.jxbrowser.navigation.LoadUrlParams
import com.teamdev.jxbrowser.net.HttpHeader
import com.teamdev.jxbrowser.net.HttpStatus
import com.teamdev.jxbrowser.net.Scheme
import com.teamdev.jxbrowser.net.UrlRequestJob
import com.teamdev.jxbrowser.net.callback.BeforeUrlRequestCallback
import com.teamdev.jxbrowser.net.callback.InterceptUrlRequestCallback.Response
import com.teamdev.jxbrowser.net.callback.VerifyCertificateCallback
import com.teamdev.jxbrowser.net.proxy.CustomProxyConfig
import com.teamdev.jxbrowser.permission.callback.RequestPermissionCallback
import com.teamdev.jxbrowser.ui.Bitmap
import com.teamdev.jxbrowser.ui.Point
import com.teamdev.jxbrowser.view.swing.BrowserView
import com.teamdev.jxbrowser.zoom.ZoomLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import okio.Path.Companion.toPath
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.createDir
import org.dweb_browser.core.std.file.ext.realFile
import org.dweb_browser.dwebview.CloseWatcher
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.dwebview.polyfill.DwebViewDesktopPolyfill
import org.dweb_browser.dwebview.polyfill.FaviconPolyfill
import org.dweb_browser.dwebview.proxy.DwebViewProxy
import org.dweb_browser.dwebview.toReadyListener
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.getOrNull
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.localViewHookExit
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.platform.desktop.webview.WebviewEngine
import org.dweb_browser.sys.device.DeviceManage
import java.util.function.Consumer
import javax.swing.SwingUtilities


class DWebViewEngine internal constructor(
  internal val remoteMM: MicroModule,
  val options: DWebViewOptions,
  internal val browser: Browser = createMainBrowser(remoteMM)
) {
  companion object {
    /**
     * 构建一个 main-browser，当 main-browser 销毁， 对应的 WebviewEngine 也会被销毁
     */
    internal fun createMainBrowser(remoteMM: MicroModule) = WebviewEngine.hardwareAccelerated {
      addScheme(Scheme.of("dweb")) { params ->
        val pureResponse = runBlocking(ioAsyncExceptionHandler) {
          remoteMM.nativeFetch(params.urlRequest().url())
        }

        val jobBuilder = UrlRequestJob.Options.newBuilder(HttpStatus.of(pureResponse.status.value))
        pureResponse.headers.forEach { (key, value) ->
          jobBuilder.addHttpHeader(HttpHeader.of(key, value))
        }

        Response.intercept(params.newUrlRequestJob(jobBuilder.build()))
      }

      addSwitch("--enable-experimental-web-platform-features")

      // 设置用户数据目录，这样WebApp退出再重新打开时能够读取之前的数据
      if (remoteMM.mmid != "desk.browser.dweb") {
        runBlocking(ioAsyncExceptionHandler) {
          if (remoteMM.createDir("/data/chromium")) {
            userDataDir(remoteMM.realFile("/data/chromium").toPath().toNioPath())
          }
        }
      }
    }.let { engine ->
      // 设置https代理
      val proxyRules = "https=${DwebViewProxy.ProxyUrl},http://127.0.0.1:17890"
      engine.proxy().config(CustomProxyConfig.newInstance(proxyRules))
      engine.network()
        .set(VerifyCertificateCallback::class.java, VerifyCertificateCallback { params ->
          // SSL Certificate to verify.
          val certificate = params.certificate()
          // FIXME 这里应该有更加严谨的证书内容判断
          if (certificate.derEncodedValue().decodeToString().contains(".dweb")) {
            VerifyCertificateCallback.Response.valid()
          } else {
            VerifyCertificateCallback.Response.defaultAction()
          }
        });

      // 拦截内部浏览器dweb deeplink跳转
      engine.network().set(BeforeUrlRequestCallback::class.java, BeforeUrlRequestCallback {
        if (it.urlRequest().url().startsWith("dweb://")) {
          remoteMM.ioAsyncScope.launch {
            remoteMM.nativeFetch(it.urlRequest().url())
          }
          BeforeUrlRequestCallback.Response.cancel()
        } else {
          BeforeUrlRequestCallback.Response.proceed()
        }
      })

      engine.permissions()
        .set(RequestPermissionCallback::class.java, RequestPermissionCallback { params, tell ->
//        if(params.permissionType() == PermissionType.GEOLOCATION) {
//          tell.grant()
//        }
          tell.grant()
        })

      val browser = engine.newBrowser()
      // 同步销毁
      browser.on(BrowserClosed::class.java) {
        engine.close()
      }
      remoteMM.ioAsyncScope.launch {
        localViewHookExit.collect {
          engine.close()
        }
      }
      browser
    }
  }

  val wrapperView: BrowserView by lazy { BrowserView.newInstance(browser) }
  val mainFrame get() = browser.mainFrame().get()
  val mainFrameOrNull get() = browser.mainFrame().getOrNull()
  val document get() = mainFrame.document().get()
  internal val mainScope = CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
  internal val ioScope = CoroutineScope(remoteMM.ioAsyncScope.coroutineContext + SupervisorJob())


  /**
   * 执行同步JS代码
   */
  fun evaluateSyncJavascriptFunctionBody(jsFunctionBody: String) =
    mainFrame.executeJavaScript<String>("String((()=>{$jsFunctionBody})())")

  /**
   * 执行异步JS代码，需要传入一个表达式
   */
  suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: (suspend () -> Unit)? = null
  ): String {
    val deferred = CompletableDeferred<String>()

    runCatching {
      mainFrame.executeJavaScript("(async()=>{return ($script)})().then(r=>JSON.stringify(r)??'undefined',e=>{throw String(e)})",
        Consumer<JsPromise> { promise ->
          promise.then {
            deferred.complete(it[0] as String)
          }.catchError {
            deferred.completeExceptionally(JsException(it[0] as String))
          }
        })
    }.getOrElse { deferred.completeExceptionally(it) }
    afterEval?.invoke()

    return deferred.await()
  }

  suspend fun loadUrl(url: String) {
    val safeUrl = resolveUrl(url)
    browser.navigation().loadUrl(safeUrl)
  }

  suspend fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
    val safeUrl = resolveUrl(url)
    val loadUrlParams = LoadUrlParams.newBuilder(safeUrl)
    additionalHttpHeaders.forEach { (key, value) ->
      loadUrlParams.addExtraHeader(HttpHeader.of(key, value))
    }

    browser.navigation().loadUrl(loadUrlParams.build())
  }

  fun resolveUrl(url: String): String {
    return url
  }

  fun getTitle(): String = browser.title()

  fun getOriginalUrl(): String = browser.url()

  fun canGoBack() = browser.navigation().canGoBack()

  fun goBack() = canGoBack().trueAlso {
    browser.navigation().goBack()
  }


  fun canGoForward() = browser.navigation().canGoForward()

  fun goForward() = canGoForward().trueAlso {
    browser.navigation().goForward()
  }

  private var faviconIcon: FaviconIcon? = null

  private class FaviconIcon(val favicon: Bitmap) {
    val imageBitmap = if (favicon.size().isEmpty) null else favicon.toImageBitmap()
  }

  fun getFavoriteIcon(): ImageBitmap? {
    val favicon = browser.favicon()
    if (faviconIcon?.favicon != favicon) {
      faviconIcon = favicon?.let { FaviconIcon(it) }
    }
    return faviconIcon?.imageBitmap
  }

  fun getCaptureImage() = browser.bitmap().toImageBitmap()

  fun requestClose() {
    browser.close(CloseOptions.newBuilder().fireBeforeUnload().build())
  }

  val destroyStateSignal = setupDestroyStateSignal(this)
  fun destroy() {
    if (destroyStateSignal.doDestroy()) {
      browser.close()
    }
  }

  fun requestUserActivation() {
    val location = Point.of(10, 10)
// Create MouseEvent with the required options.
    val mouseClickEvent = document.createMouseEvent(
      EventType.CLICK, MouseEventParams.newBuilder()
        // The main button pressed.
        .button(MouseEvent.Button.MAIN).clientLocation(location).screenLocation(location)
        .uiEventModifierParams(
          UiEventModifierParams.newBuilder().eventParams(
            EventParams.newBuilder().bubbles(true).cancelable(true).build()
          ).build()
        ).build()
    )

    val event2 = document.createEvent(
      EventType.FOCUS, EventParams.newBuilder().bubbles(true).cancelable(true).build()
    )

    document.dispatch(mouseClickEvent)
//    browser.dispatch(MousePressed.newBuilder(Point.of(1, 1)).build())
  }

  private var whenInjectFrame: Frame? = null

  private val injectJsActionList = mutableListOf<Frame.() -> Unit>().also { actionList ->
    browser.set(InjectJsCallback::class.java, InjectJsCallback { event ->
      debugDWebView("InjectJsCallback start")
      val frame = event.frame()
      val safeList = actionList.toList()/// 拷贝一份静态的
      whenInjectFrame = frame
      for (action in safeList) {
        runCatching {
          frame.action()
        }.getOrElse { debugDWebView("InjectJsCallback error", frame, it) }
      }
      debugDWebView("InjectJsCallback end")
      InjectJsCallback.Response.proceed()
    })
  }

  fun injectJsAction(action: Frame.() -> Unit) {
    injectJsActionList.add(action)
    /// 如果frame合适，那么就直接执行
    when (val frame = mainFrameOrNull) {
      whenInjectFrame -> frame?.action()
    }
  }

  fun addDocumentStartJavaScript(script: String) {
    injectJsAction {
      evaluateSyncJavascriptFunctionBody(script)
    }
  }

  private val jsInterfaces by lazy {
    mutableMapOf<String, Any>().also { injectInterfaces ->
      injectJsAction {
        val window = window()
        for ((name, obj) in injectInterfaces) {
          window.setProp(name, obj)
        }
      }
    }
  }

  fun addJavascriptInterface(obj: Any, name: String) {
    jsInterfaces[name] = obj
  }

  private fun setUA() {
    val brandList = mutableListOf<IDWebView.UserAgentBrandData>()
    IDWebView.brands.forEach {
      brandList.add(
        IDWebView.UserAgentBrandData(
          it.brand, if (it.version.contains(".")) it.version.split(".").first() else it.version
        )
      )
    }

    val versionName = DeviceManage.deviceAppVersion()
    brandList.add(IDWebView.UserAgentBrandData("DwebBrowser", versionName.split(".").first()))

    // 新版的chrome可以delete brands 然后重新赋值
    addDocumentStartJavaScript(
      DwebViewDesktopPolyfill.UserAgentData +
          // 执行脚本
          ";NavigatorUAData.__upsetBrands__(${JsonLoose.encodeToString(brandList)});"
    )
  }

  private var verticalScrollBarVisible = true
  private var horizontalScrollBarVisible = true
  private val scrollBarsVisible get() = verticalScrollBarVisible || horizontalScrollBarVisible

  fun setVerticalScrollBarVisible(visible: Boolean) {
    verticalScrollBarVisible = visible
    effectScrollBarsVisible()
  }

  fun setHorizontalScrollBarVisible(visible: Boolean) {
    horizontalScrollBarVisible = visible
    effectScrollBarsVisible()
  }

  private fun effectScrollBarsVisible() {
    browser.settings().apply {
      when {
        scrollBarsVisible -> if (scrollbarsHidden()) {
          debugDWebView("effectScrollBarsVisible", "show")
          showScrollbars()
        }

        else -> if (!scrollbarsHidden()) {
          debugDWebView("effectScrollBarsVisible", "hide")
          hideScrollbars()
        }
      }
    }
  }

  fun setContentScale(scale: Double) {
    browser.zoom().apply { enable(); level(ZoomLevel.of(scale)) }
  }

  val loadStateChangeSignal = setupLoadStateChangeSignal(this)
  val onReady by lazy { loadStateChangeSignal.toReadyListener() }
  val scrollSignal = setupScrollSignal(this)
  val dwebFavicon = FaviconPolyfill(this)
  private val _setupCreateWindowSignals = setupCreateWindowSignals(this)
  val beforeCreateWindowSignal = _setupCreateWindowSignals.beforeCreateWindowSignal
  val createWindowSignal = _setupCreateWindowSignals.createWindowSignal
  val closeWatcher = CloseWatcher(this)
  val beforeUnloadSignal = setupBeforeUnloadSignal(this)
  val loadingProgressSharedFlow = setupLoadingProgressSharedFlow(this)
  val downloadSignal = setupDownloadSignal(this)

  init {

    setUA()

    // 设置
    browser.settings().apply {
      enableJavaScript()
      enableLocalStorage()
      enableImages()
      // 关闭此选项，否则会导致 windows 平台RenderMode异常 https://github.com/flutter/flutter-intellij/pull/4804
      if (PureViewController.isMacOS) {
        enableTransparentBackground()
      }
      enableOverscrollHistoryNavigation()
      allowRunningInsecureContent()
      allowJavaScriptAccessClipboard()
      allowScriptsToCloseWindows()
      allowLoadingImagesAutomatically()
    }

    if (debugDWebView.isEnable) {
      browser.on(ConsoleMessageReceived::class.java) { event ->
        val consoleMessage = event.consoleMessage()
        val level = consoleMessage.level()
        val message = consoleMessage.message()
        val lineNumber = consoleMessage.lineNumber()
        val source = consoleMessage.source()
        when (level) {
          ConsoleMessageLevel.LEVEL_ERROR -> debugDWebView(
            "JsConsole/$level",
            message,
            "<$source:$lineNumber>",
          )

          else -> debugDWebView("JsConsole/$level", message)
        }
      }
    }
    // 创建menu,初始化就创建，而不是监听的时候
    val (popupMenu, clickEffect) = browser.createRightClickMenu(ioScope)
    // 监听右击事件
    browser.set(ShowContextMenuCallback::class.java, ShowContextMenuCallback { params, tell ->
      // 监听回调的点击事件
      clickEffect.onEach {
        if (!tell.isClosed) tell.close()
      }.launchIn(ioScope)
      // 创建事件调度线程，所有跟用户界面有关的代码都应当在这个线程上运行
      SwingUtilities.invokeLater {
        // 在指定位置显示上下文菜单。
        val location = params.location()
        popupMenu.show(wrapperView, location.x(), location.y())
      }
    })

    if (options.url.isNotEmpty()) {
      ioScope.launch {
        loadUrl(options.url)
      }
    }
  }


}

