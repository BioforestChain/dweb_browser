package org.dweb_browser.dwebview.engine

import androidx.compose.ui.graphics.ImageBitmap
import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.browser.CloseOptions
import com.teamdev.jxbrowser.browser.event.BrowserClosed
import com.teamdev.jxbrowser.browser.internal.rpc.ConsoleMessageReceived
import com.teamdev.jxbrowser.js.JsException
import com.teamdev.jxbrowser.js.JsPromise
import com.teamdev.jxbrowser.navigation.LoadUrlParams
import com.teamdev.jxbrowser.navigation.event.FrameLoadFailed
import com.teamdev.jxbrowser.navigation.event.FrameLoadFinished
import com.teamdev.jxbrowser.navigation.event.NavigationFinished
import com.teamdev.jxbrowser.navigation.event.NavigationStarted
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
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.WebLoadErrorState
import org.dweb_browser.dwebview.WebLoadStartState
import org.dweb_browser.dwebview.WebLoadState
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.dwebview.base.LoadedUrlCache
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.dwebview.proxy.DwebViewProxy
import org.dweb_browser.helper.JsonLoose
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.platform.desktop.webview.WebviewEngine
import org.dweb_browser.sys.device.DeviceManage
import java.util.function.Consumer

class DWebViewEngine internal constructor(
  internal val remoteMM: MicroModule,
  val options: DWebViewOptions
) {


  internal val browser: Browser = WebviewEngine.hardwareAccelerated {
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
      mainFrame.executeJavaScript(
        "(async()=>{return ($script)})().then(r=>JSON.stringify(r),e=>{throw String(e)})",
        Consumer<JsPromise> { promise ->
          promise
            .then {
              deferred.complete(it[0] as String)
            }
            .catchError {
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
    url: String,
    additionalHttpHeaders: MutableMap<String, String>,
    postData: String? = null
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

  suspend fun goBack(): Boolean = withMainContext {
    if (canGoBack()) {
      browser.navigation().goBack()
      true
    } else {
      false
    }
  }


  fun canGoForward() = browser.navigation().canGoForward()

  suspend fun goForward() = withMainContext {
    if (canGoForward()) {
      browser.navigation().goForward()
      true
    } else {
      false
    }
  }

  suspend fun getFavoriteIcon(): ImageBitmap = browser.favicon().pixels().toImageBitmap()

  fun destroy() {
    browser.close(CloseOptions.newBuilder().build())
  }


  private val documentStartJsList by lazy {
    mutableListOf<String>().also { scriptList ->
      // url导航结束，也就是dom开始加载时，执行 startScript
      browser.navigation().on(NavigationFinished::class.java) {
        for (script in scriptList) {
          evaluateSyncJavascriptFunctionBody(script)
        }
      }
    }
  }

  fun addDocumentStartJavaScript(script: String) {
    documentStartJsList += script
  }

  private fun setUA() {
    val brandList = mutableListOf<IDWebView.UserAgentBrandData>()
    IDWebView.brands.forEach {
      brandList.add(
        IDWebView.UserAgentBrandData(
          it.brand,
          if (it.version.contains(".")) it.version.split(".").first() else it.version
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

  val loadStateChangeSignal = Signal<WebLoadState>().also { signal ->
    fun emitSignal(state: WebLoadState) {
      ioScope.launch {
        signal.emit(state)
      }
    }
    browser.navigation().apply {
      on(NavigationStarted::class.java) {
        if (it.isInMainFrame) {
          emitSignal(WebLoadStartState(it.url()))
        }
      }
      on(FrameLoadFinished::class.java) {
        if (it.frame() == mainFrame) {
          emitSignal(WebLoadSuccessState(it.url()))
        }
      }
      on(FrameLoadFailed::class.java) {
        if (it.frame() == mainFrame) {
          emitSignal(WebLoadErrorState(it.url(), it.error().name))
        }
      }
    }
  }

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