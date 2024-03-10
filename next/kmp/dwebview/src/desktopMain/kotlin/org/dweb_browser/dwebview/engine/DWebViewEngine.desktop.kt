package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.browser.CloseOptions
import com.teamdev.jxbrowser.browser.callback.InjectJsCallback
import com.teamdev.jxbrowser.browser.event.BrowserClosed
import com.teamdev.jxbrowser.browser.internal.rpc.ConsoleMessageReceived
import com.teamdev.jxbrowser.frame.Frame
import com.teamdev.jxbrowser.js.JsException
import com.teamdev.jxbrowser.js.JsPromise
import com.teamdev.jxbrowser.navigation.LoadUrlParams
import com.teamdev.jxbrowser.net.HttpHeader
import com.teamdev.jxbrowser.net.HttpStatus
import com.teamdev.jxbrowser.net.Scheme
import com.teamdev.jxbrowser.net.UrlRequestJob
import com.teamdev.jxbrowser.net.callback.InterceptUrlRequestCallback.Response
import com.teamdev.jxbrowser.net.proxy.CustomProxyConfig
import com.teamdev.jxbrowser.view.swing.BrowserView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.encodeToJsonElement
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.CloseWatcher
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.base.LoadedUrlCache
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.dwebview.polyfill.FaviconPolyfill
import org.dweb_browser.dwebview.proxy.DwebViewProxy
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.helper.trueAlso
import org.dweb_browser.platform.desktop.webview.WebviewEngine
import org.dweb_browser.sys.device.DeviceManage
import java.util.function.Consumer

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
    }.let { engine ->
      // 设置https代理
      val proxyRules = "https=${DwebViewProxy.ProxyUrl}"
      engine.proxy().config(CustomProxyConfig.newInstance(proxyRules))
      val browser = engine.newBrowser()
      // 同步销毁
      browser.on(BrowserClosed::class.java) {
        engine.close()
      }
      browser
    }
  }

  val wrapperView: BrowserView by lazy { BrowserView.newInstance(browser) }
  val mainFrame get() = browser.mainFrame().get()
  internal val mainScope = CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
  internal val ioScope = CoroutineScope(remoteMM.ioAsyncScope.coroutineContext + SupervisorJob())
  internal val loadedUrlCache = LoadedUrlCache(ioScope)


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
    loadedUrlCache.checkLoadedUrl(safeUrl) {
      ioScope.launch {
        browser.navigation().loadUrl(url)
      }.join()
      true
    }
  }

  suspend fun loadUrl(
    url: String, additionalHttpHeaders: MutableMap<String, String>, postData: String? = null
  ) {
    val safeUrl = resolveUrl(url)
    loadedUrlCache.checkLoadedUrl(safeUrl, additionalHttpHeaders) {
      val loadUrlParams = LoadUrlParams.newBuilder(url)
      additionalHttpHeaders.forEach { (key, value) ->
        loadUrlParams.addExtraHeader(HttpHeader.of(key, value))
      }

      if (postData != null) {
        loadUrlParams.postData(postData)
      }

      ioScope.launch {
        browser.navigation().loadUrl(loadUrlParams.build())
      }.join()
      true
    }
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

  fun getFavoriteIcon() = runCatching {
    /// toImageBitmap 可能会解析异常，所以需要放在 runCatching 里头
    browser.favicon().pixels().toImageBitmap()
  }.getOrNull()

  fun destroy() {
    browser.close(CloseOptions.newBuilder().build())
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
    when (val frame = mainFrame) {
      whenInjectFrame -> frame.action()
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
          window.putProperty(name, obj)
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
      """(()=>{
        const uaBrands = navigator.userAgentData.brands
        delete NavigatorUAData.prototype.brands;
        Object.defineProperty(NavigatorUAData.prototype,'brands',{value:uaBrands.concat(${
        JsonLoose.encodeToJsonElement(brandList)
      })})
      })()""".trimIndent()
    )
  }

  val loadStateChangeSignal = setupLoadStateChangeSignal(this)
  val scrollSignal = setupScrollSignal(this)
  val dwebFavicon = FaviconPolyfill(this)
  private val _setupCreateWindowSignals = setupCreateWindowSignals(this)
  val beforeCreateWindowSignal = _setupCreateWindowSignals.beforeCreateWindowSignal
  val createWindowSignal = _setupCreateWindowSignals.createWindowSignal
  val closeWatcher = CloseWatcher(this)

  init {

    setUA()

    // 设置
    browser.settings().apply {
      enableJavaScript()
      enableLocalStorage()
      enableImages()
      enableTransparentBackground()
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
        debugDWebView("JsConsole/$level", message)
      }
    }
  }
}