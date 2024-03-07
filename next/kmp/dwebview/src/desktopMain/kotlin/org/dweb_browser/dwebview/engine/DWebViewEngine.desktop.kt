package org.dweb_browser.dwebview.engine

import androidx.compose.ui.graphics.ImageBitmap
import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.browser.CloseOptions
import com.teamdev.jxbrowser.browser.event.BrowserClosed
import com.teamdev.jxbrowser.js.JsException
import com.teamdev.jxbrowser.js.JsPromise
import com.teamdev.jxbrowser.navigation.LoadUrlParams
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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.encodeToJsonElement
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.base.LoadedUrlCache
import org.dweb_browser.dwebview.polyfill.UserAgentData
import org.dweb_browser.dwebview.proxy.DwebViewProxy
import org.dweb_browser.helper.JsonLoose
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
  private val dwebviewEngine by lazy {
    WebviewEngine.hardwareAccelerated {
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
    }
  }

  private val browser: Browser = dwebviewEngine.newBrowser()
  val wrapperView: BrowserView by lazy { BrowserView.newInstance(browser) }
  val mainFrame get() = browser.mainFrame().get()
  internal val mainScope = CoroutineScope(mainAsyncExceptionHandler + SupervisorJob())
  internal val ioScope = CoroutineScope(remoteMM.ioAsyncScope.coroutineContext + SupervisorJob())
  internal val loadedUrlCache = LoadedUrlCache(ioScope)

  init {
    // 设置https代理
    val proxyRules = "https=${DwebViewProxy.ProxyUrl}"
    dwebviewEngine.proxy().config(CustomProxyConfig.newInstance(proxyRules))

    // 添加监听事件
    addListenerEvent()

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
  }

  private fun addListenerEvent() {
    // 开始加载时，设置userAgent
    browser.navigation().on(NavigationStarted::class.java) {
      setUA()
    }

    // browser关闭，销毁dwebviewEngine
    browser.on(BrowserClosed::class.java) {
      dwebviewEngine.close()
    }
  }

  /**
   * 执行同步JS代码
   */
  fun evaluateSyncJavascriptCode(script: String) =
    mainFrame.executeJavaScript<String>(script)

  /**
   * 执行异步JS代码，需要传入一个表达式
   */
  suspend fun evaluateAsyncJavascriptCode(
    script: String, afterEval: (suspend () -> Unit)? = null
  ): String {
    val deferred = CompletableDeferred<String>()

    runCatching {
      mainFrame.executeJavaScript(
        "(async()=>{return ($script)})().then(r=>JSON.stringify(r),e=>String(e))",
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

  fun loadUrl(url: String) {
    val safeUrl = resolveUrl(url)
    loadedUrlCache.checkLoadedUrl(safeUrl) {
      browser.navigation().loadUrl(url)
      true
    }
  }

  fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String, String>, postData: String? = null) {
    val safeUrl = resolveUrl(url)
    loadedUrlCache.checkLoadedUrl(safeUrl, additionalHttpHeaders) {
      val loadUrlParams = LoadUrlParams.newBuilder(url)
      additionalHttpHeaders.forEach { (key, value) ->
        loadUrlParams.addExtraHeader(HttpHeader.of(key, value))
      }

      if(postData != null) {
        loadUrlParams.postData(postData)
      }

      browser.navigation().loadUrl(loadUrlParams.build())
      true
    }
  }

  fun resolveUrl(url: String): String {
    return url
  }

  fun getTitle() = browser.title()

  fun getOriginalUrl() = browser.url()

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
    if(canGoForward()) {
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

    evaluateSyncJavascriptCode(
      """
        ${UserAgentData.polyfillScript}
        let userAgentData = ""
        if (!navigator.userAgentData) {
         userAgentData  = new NavigatorUAData(navigator, ${
        JsonLoose.encodeToJsonElement(
          brandList
        )
      });
        } else {
          userAgentData =
           new NavigatorUAData(navigator, 
           navigator.userAgentData.brands.concat(
           ${
        JsonLoose.encodeToJsonElement(
          brandList
        )
      }));
        }
        Object.defineProperty(Navigator.prototype, 'userAgentData', {
          enumerable: true,
          configurable: true,
          get: function getUseAgentData() {
            return userAgentData;
          }
        });
        Object.defineProperty(window, 'NavigatorUAData', {
          enumerable: false,
          configurable: true,
          writable: true,
          value: NavigatorUAData
        });
      """.trimIndent()
    )
  }
}