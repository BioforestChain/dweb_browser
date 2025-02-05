package org.dweb_browser.dwebview.engine

import androidx.compose.ui.graphics.ImageBitmap
import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.browser.CloseOptions
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback
import com.teamdev.jxbrowser.browser.event.BrowserClosed
import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived
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
import com.teamdev.jxbrowser.net.callback.VerifyCertificateCallback
import com.teamdev.jxbrowser.net.proxy.CustomProxyConfig
import com.teamdev.jxbrowser.permission.callback.RequestPermissionCallback
import com.teamdev.jxbrowser.ui.Point
import com.teamdev.jxbrowser.view.compose.BrowserViewState
import com.teamdev.jxbrowser.zoom.ZoomLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import kotlinx.serialization.json.Json
import org.dweb_browser.core.http.dwebProxyService
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.CloseWatcher
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.UrlLoadingPolicy
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.dwebview.engine.decidePolicyHook.hookCloseWatcher
import org.dweb_browser.dwebview.engine.decidePolicyHook.hookDeeplink
import org.dweb_browser.dwebview.polyfill.DwebViewDesktopPolyfill
import org.dweb_browser.dwebview.polyfill.FaviconPolyfill
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.getOrNull
import org.dweb_browser.helper.platform.getOrCreateIncognitoProfile
import org.dweb_browser.helper.platform.getOrCreateProfile
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.helper.platform.webViewEngine
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.helper.utf8String
import org.dweb_browser.helper.utf8ToBase64UrlString
import org.dweb_browser.sys.device.DeviceManage
import java.awt.Window
import java.util.function.Consumer
import kotlin.system.exitProcess


class DWebViewEngine internal constructor(
  internal val remoteMM: MicroModule.Runtime,
  val options: DWebViewOptions,
  internal val browser: Browser = createMainBrowser(remoteMM, options),
) {
  companion object {
    /**
     * 构建一个 main-browser，当 main-browser 销毁， 对应的 WebviewEngine 也会被销毁
     */
    internal fun createMainBrowser(
      remoteMM: MicroModule.Runtime,
      options: DWebViewOptions,
    ): Browser = when {
      options.enabledOffScreenRender -> webViewEngine.offScreenEngine
      else -> webViewEngine.hardwareAcceleratedEngine
    }.let { engine ->
      val profiles = engine.profiles()
      val profileName = "${remoteMM.mmid}/${options.profile.utf8ToBase64UrlString}"
      val profile = when (val sessionId = options.incognitoSessionId) {
        null -> profiles.getOrCreateProfile(profileName)
        else -> profiles.getOrCreateIncognitoProfile(profileName, sessionId)
      }
      profile
    }.let { profile ->
      // 设置https代理
      // TODO 这里也许需要支持动态变更，但目前好像没有这个需求
      val proxyRules = "https=${dwebProxyService.proxyUrl.value}"
      profile.proxy().config(CustomProxyConfig.newInstance(proxyRules))
      profile.network()
        .set(VerifyCertificateCallback::class.java, VerifyCertificateCallback { params ->
          // SSL Certificate to verify.
          val certificate = params.certificate()
          val host = params.host().value()
          // 放行的条件
          val isPrivateNetwork = isPrivateNetwork(host)
          val isLocalHost = (host == "127.0.0.1" || host == "localhost")
          // FIXME 这里应该有更加严谨的证书内容判断
          if (certificate.derEncodedValue().utf8String.contains(".dweb")
            || isPrivateNetwork
            || isLocalHost
          ) {
            VerifyCertificateCallback.Response.valid()
          } else {
            VerifyCertificateCallback.Response.defaultAction()
          }
        });

      //TODO 这里是还没做完的桌面端获取地址，改成ip位置？
      profile.permissions()
        .set(RequestPermissionCallback::class.java, RequestPermissionCallback { params, tell ->
//        if(params.permissionType() == PermissionType.GEOLOCATION) {
//          tell.grant()
//        }
          tell.grant()
        })

      val browser = profile.newBrowser()
      // 同步销毁
      browser.on(BrowserClosed::class.java) {
        profile.browsers().isEmpty().trueAlso {
          val engine = profile.engine()
          val isEngineEmptyBrowser = engine.profiles().list().all { it.browsers().isEmpty() }
          if (isEngineEmptyBrowser) {
            engine.close()
          }
        }
      }
      browser
    }

    /**工具方法：检查是否属于私有网段*/
    private fun isPrivateNetwork(ip: String): Boolean {
      return ip.startsWith("10.")
          || ip.startsWith("192.168.")
          || ip.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"))
    }
  }


  fun getBrowserViewState(window: Window): BrowserViewState {
    return BrowserViewState(browser, lifecycleScope, window)
  }


  val mainFrame
    get() = kotlin.runCatching { browser.mainFrame().get() }.getOrElse {
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
                deferred.completeExceptionally(JsException("result error: ${result.substring(1)}"))
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

  fun loadUrl(url: String) {
    val safeUrl = resolveUrl(url)
    browser.navigation().apply {
      when {
        canGoBack() -> loadUrl(safeUrl)
        // 因为桌面端的起始url是 about:blank ，所以这里用 replace 来加载出事页面
        url == "about:blank" -> replaceUrl(url)
        else -> loadUrl(safeUrl)
      }
    }
  }

  private fun replaceUrl(url: String) {
    evaluateSyncJavascriptFunctionBody("location.replace(${Json.encodeToString(url)})")
  }

  fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>) {
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

  fun canGoBack() = browser.navigation().let {
    // browser默认会先加载about:blank，导致仅根据canGoBack判断时，返回值为true，执行goBack会显示about:blank空白页
    it.canGoBack() && it.currentEntryIndex() > 1
  }

  fun goBack() = canGoBack().trueAlso {
    browser.navigation().goBack()
  }

  fun canGoForward() = browser.navigation().canGoForward()

  fun goForward() = canGoForward().trueAlso {
    browser.navigation().goForward()
  }

  fun getFavoriteIcon(): ImageBitmap? {
    return iconBitmapFlow.value
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

  val loadStateFlow = setupLoadStateFlow(this, options.url)
  val scrollSignal = setupScrollSignal(this)
  val titleFlow = setupTitleFlow(this)
  val dwebFavicon = FaviconPolyfill(this)
  private val _setupCreateWindowSignals = setupCreateWindowSignals(this)
  val createWindowSignal = _setupCreateWindowSignals.createWindowSignal
  val closeWatcher = CloseWatcher(this)
  val beforeUnloadSignal = setupBeforeUnloadSignal(this)
  val loadingProgressStateFlow = setupLoadingProgressStateFlow(this)
  val downloadSignal = setupDownloadSignal(this)
  internal val createWebMessagePortPicker = setupWebMessagePicker(this)
  val iconBitmapFlow = setupIconBitmapFlow(this)
  internal val fileChooser = setupFileChooser(this)
  val overrideUrlLoadingHooks by lazy { setupOverrideUrlLoadingHooks(this) }
  val decidePolicyForCreateWindowHooks = mutableListOf<(url: String) -> UrlLoadingPolicy>()

  init {

    setUA()

    //#region
    hookDeeplink()
    hookCloseWatcher()
    //#endregion

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

    if (options.url.isNotEmpty()) {
      loadUrl(options.url)
    }
    if (options.openDevTools) {
      browser.devTools().show()
    }
  }
}

