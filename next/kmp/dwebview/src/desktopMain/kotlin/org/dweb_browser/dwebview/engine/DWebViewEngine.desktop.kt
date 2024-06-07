package org.dweb_browser.dwebview.engine

import androidx.compose.ui.graphics.ImageBitmap
import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.browser.CloseOptions
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback
import com.teamdev.jxbrowser.browser.callback.ShowContextMenuCallback
import com.teamdev.jxbrowser.browser.event.BrowserClosed
import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived
import com.teamdev.jxbrowser.dom.event.EventParams
import com.teamdev.jxbrowser.dom.event.EventType
import com.teamdev.jxbrowser.dom.event.MouseEvent
import com.teamdev.jxbrowser.dom.event.MouseEventParams
import com.teamdev.jxbrowser.dom.event.UiEventModifierParams
import com.teamdev.jxbrowser.engine.EngineOptions
import com.teamdev.jxbrowser.frame.Frame
import com.teamdev.jxbrowser.js.ConsoleMessageLevel
import com.teamdev.jxbrowser.js.JsException
import com.teamdev.jxbrowser.js.JsPromise
import com.teamdev.jxbrowser.navigation.LoadUrlParams
import com.teamdev.jxbrowser.net.HttpHeader
import com.teamdev.jxbrowser.net.HttpStatus
import com.teamdev.jxbrowser.net.Scheme
import com.teamdev.jxbrowser.net.UrlRequestJob
import com.teamdev.jxbrowser.net.callback.InterceptUrlRequestCallback.Response
import com.teamdev.jxbrowser.net.callback.VerifyCertificateCallback
import com.teamdev.jxbrowser.net.proxy.CustomProxyConfig
import com.teamdev.jxbrowser.permission.callback.RequestPermissionCallback
import com.teamdev.jxbrowser.ui.Bitmap
import com.teamdev.jxbrowser.ui.Point
import com.teamdev.jxbrowser.view.swing.BrowserView
import com.teamdev.jxbrowser.zoom.ZoomLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.serialization.encodeToString
import okio.Path
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.ext.createDir
import org.dweb_browser.core.std.file.ext.realPath
import org.dweb_browser.dwebview.CloseWatcher
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.dwebview.polyfill.DwebViewDesktopPolyfill
import org.dweb_browser.dwebview.polyfill.FaviconPolyfill
import org.dweb_browser.dwebview.proxy.DwebViewProxy
import org.dweb_browser.dwebview.toReadyListener
import org.dweb_browser.helper.ENV_SWITCH_KEY
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.ReasonLock
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.envSwitch
import org.dweb_browser.helper.getOrNull
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.platform.desktop.webview.WebviewEngine
import org.dweb_browser.sys.device.DeviceManage
import java.util.function.Consumer
import javax.swing.SwingUtilities
import kotlin.system.exitProcess


class DWebViewEngine internal constructor(
  internal val remoteMM: MicroModule.Runtime,
  val dataDir: Path,
  val options: DWebViewOptions,
  internal val browser: Browser = createMainBrowser(
    remoteMM, dataDir, options.enabledOffScreenRender
  ),
) {
  companion object {
    // userDataDir同一个engine不能多次调用，jxbrowser会弹出异常无法捕获
    private val userDataDirectoryInUseMicroModuleSet = mutableMapOf<String, okio.Path>()
    private val userDataDirectoryLocks = ReasonLock()

    internal suspend fun prepareDataDir(remoteMM: MicroModule.Runtime, subDataDirName: String?) =
      userDataDirectoryLocks.withLock("${remoteMM.mmid}/$subDataDirName") {
        userDataDirectoryInUseMicroModuleSet.getOrPut("${remoteMM.mmid}/$subDataDirName") {
          val dirName = when (subDataDirName) {
            null -> "/data/dwebview"
            else -> "/data/dwebview-${subDataDirName.encodeURIComponent()}"
          }
          remoteMM.createDir(dirName)
          remoteMM.realPath(dirName)
        }
      }

    /**
     * 构建一个 main-browser，当 main-browser 销毁， 对应的 WebviewEngine 也会被销毁
     */
    internal fun createMainBrowser(
      remoteMM: MicroModule.Runtime,
      dataDir: Path,
      enabledOffScreenRender: Boolean,
    ): Browser {
      val optionsBuilder: EngineOptions.Builder.() -> Unit = {
        // 拦截dweb deeplink
        addScheme(Scheme.of("dweb")) { params ->
          remoteMM.scopeLaunch(cancelable = true) {
            val res = remoteMM.nativeFetch(params.urlRequest().url())
            // TODO eval navigator.dweb.dispatchEvent(new CustomEvent("dweb-deeplink"))
          }

          val job = params.newUrlRequestJob(
            UrlRequestJob.Options //
              .newBuilder(HttpStatus.OK) //
              .addHttpHeader(HttpHeader.of("Access-Control-Allow-Origin", "*")) //
              .build() //
          )
          job.write(byteArrayOf())
          job.complete()
          Response.intercept(job)
        }

        addSwitch("--enable-experimental-web-platform-features")
      }

      val dataNioDir = dataDir.toNioPath()

      return if (enabledOffScreenRender) {
        WebviewEngine.offScreen(dataNioDir, optionsBuilder)
      } else {
        WebviewEngine.hardwareAccelerated(dataNioDir, optionsBuilder)
      }.let { engine ->
        // 设置https代理
        val proxyRules = "https=${DwebViewProxy.ProxyUrl}"
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

        //TODO 这里是还没做完的桌面端获取地址，改成ip位置？
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
          userDataDirectoryInUseMicroModuleSet.remove(remoteMM.mmid)
          engine.browsers().isEmpty().trueAlso { engine.close() }
        }
//        remoteMM.scopeLaunch(cancelable = true) {
//          remoteMM.nativeFetch(URLBuilder("file://tray.sys.dweb/registry").apply {
//            parameters["title"] = "Exit App"
//            parameters["url"] = ""
//          }.buildUnsafeString())
//        }
        browser
      }
    }
  }

  val wrapperView: BrowserView by lazy { BrowserView.newInstance(browser) }

  val mainFrame get() = kotlin.runCatching { browser.mainFrame().get() }.getOrElse {
    // 在开启到一半强制结束程序到时候，释放子线程
    exitProcess(0)
  }
  val mainFrameOrNull get() = browser.mainFrame().getOrNull()
  val document get() = mainFrame.document().get()

  internal val lifecycleScope = remoteMM.getRuntimeScope() + SupervisorJob()

  /**
   * 执行同步JS代码
   */
  fun evaluateSyncJavascriptFunctionBody(jsFunctionBody: String) =
    mainFrame.executeJavaScript<String>("String((()=>{$jsFunctionBody})())")

  /**
   * 执行异步JS代码，需要传入一个表达式
   */
  suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: (suspend () -> Unit)? = null,
  ): String {
    val deferred = CompletableDeferred<String>()
    runCatching {
      mainFrame.executeJavaScript("""
        (async () => {
          try {
            return "1" + String(JSON.stringify(await $script));
          } catch (e) {
            return "0" + String(e);
          }
        })();
      """.trimIndent(), Consumer<JsPromise> { jsObject ->
        if (jsObject == null) {
          deferred.completeExceptionally(JsException("maybe SyntaxError"))
          return@Consumer
        }
        runCatching {
          jsObject.then {
            runCatching {
              val result = it[0] as String
              if (result.first() == '1') {
                deferred.complete(result.substring(1))
              } else {
                deferred.completeExceptionally(JsException(result.substring(1)))
              }
            }.getOrElse { deferred.completeExceptionally(it) }
          }
        }.getOrElse { deferred.completeExceptionally(it) }
      })
    }.getOrElse {
      deferred.completeExceptionally(it)
    }
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
  internal val createWebMessagePortPicker = setupWebMessagePicker(this)

  init {

    setUA()

    // 设置
    browser.settings().apply {
      enableJavaScript()
      enableLocalStorage()
      enableImages()
      // 关闭此选项，否则会导致 windows 平台RenderMode异常 https://github.com/flutter/flutter-intellij/pull/4804
      if (envSwitch.isEnabled(ENV_SWITCH_KEY.DWEBVIEW_ENABLE_TRANSPARENT_BACKGROUND) || options.enabledOffScreenRender) {
        enableTransparentBackground()
      }
      enableOverscrollHistoryNavigation()
      allowRunningInsecureContent()
      allowJavaScriptAccessClipboard()
      allowScriptsToCloseWindows()
      allowLoadingImagesAutomatically()
    }

    if (debugDWebView.isEnable && envSwitch.isEnabled(ENV_SWITCH_KEY.DWEBVIEW_JS_CONSOLE)) {
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

          ConsoleMessageLevel.WARNING -> debugDWebView(
            "JsConsole/$level", "$message <$source:$lineNumber>"
          )

          else -> debugDWebView.verbose("JsConsole/$level", "$message <$source:$lineNumber>")
        }
      }
    }
    // 创建menu,初始化就创建，而不是监听的时候
    val (popupMenu, clickEffect) = browser.createRightClickMenu(lifecycleScope)
    // 监听右击事件
    browser.set(ShowContextMenuCallback::class.java, ShowContextMenuCallback { params, tell ->
      // 监听回调的点击事件
      clickEffect.onEach {
        if (!tell.isClosed) tell.close()
      }.launchIn(lifecycleScope)
      // 创建事件调度线程，所有跟用户界面有关的代码都应当在这个线程上运行
      SwingUtilities.invokeLater {
        // 在指定位置显示上下文菜单。
        val location = params.location()
        popupMenu.show(wrapperView, location.x(), location.y())
      }
    })

    if (options.url.isNotEmpty()) {
      lifecycleScope.launch {
        loadUrl(options.url)
      }
    }
    if (options.openDevTools) {
      browser.devTools().show()
    }
  }


}

