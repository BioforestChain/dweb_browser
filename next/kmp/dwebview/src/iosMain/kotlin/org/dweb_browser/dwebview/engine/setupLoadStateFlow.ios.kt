package org.dweb_browser.dwebview.engine

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.dweb_browser.dwebview.WebLoadErrorState
import org.dweb_browser.dwebview.WebLoadStartState
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.dwebview.polyfill.DwebViewIosPolyfill
import org.dweb_browser.helper.collectIn
import org.dweb_browser.platform.ios.KeyValueObserverProtocol
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSString
import platform.Foundation.addObserver
import platform.Foundation.removeObserver
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

internal fun setupLoadStateFlow(
  engine: DWebViewEngine,
  dwebNavigationDelegate: DWebNavigationDelegate,
  urlObserver: DWebUrlObserver,
  configuration: WKWebViewConfiguration,
  initUrl: String,
) = MutableStateFlow(
  when (initUrl) {
    "", "about:blank" -> WebLoadSuccessState("about:blank")
    else -> WebLoadStartState(initUrl)
  }
).also { flow ->
  dwebNavigationDelegate.didStartProvisionalNavigationHooks.add {
    flow.emit(WebLoadStartState(loadedUrl))
  }
  dwebNavigationDelegate.didFinishNavigationHooks.add {
    flow.emit(WebLoadSuccessState(loadedUrl))
  }
  dwebNavigationDelegate.didFailNavigationHooks.add {
    val errorMessage = "[${withError.code}]$currentUrl\n${withError.description}"
    flow.emit(WebLoadErrorState(currentUrl, errorMessage))
  }
  urlObserver.urlFlow.collectIn(engine.lifecycleScope) {
    debugDWebView("urlObserver", it)
    if (flow.value is WebLoadSuccessState) {
      flow.value = WebLoadSuccessState(it)
    }
  }

  configuration.userContentController.apply {
    ifNoDefineUserScript(DwebViewIosPolyfill.NavigationHook) {
      addUserScript(
        WKUserScript(
          source = DwebViewIosPolyfill.NavigationHook,
          injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentStart,
          forMainFrameOnly = false,
        )
      )
      addScriptMessageHandler(
        scriptMessageHandler = NavigationChangeMessageHandler(engine),
        name = "navigationChange",
      )
    }
  }
}

internal class NavigationChangeMessageHandler(private val engine: DWebViewEngine) : NSObject(),
  WKScriptMessageHandlerProtocol {
  override fun userContentController(
    userContentController: WKUserContentController,
    didReceiveScriptMessage: WKScriptMessage,
  ) {
    val href = didReceiveScriptMessage.body as String
    debugDWebView("hashChange", href)
    engine.loadStateFlow.value = WebLoadSuccessState(href)
  }
}

/**
 * 监听加载进度
 */
@OptIn(ExperimentalForeignApi::class)
internal class DWebUrlObserver(val engine: DWebViewEngine) : NSObject(),
  KeyValueObserverProtocol {
  init {
    engine.addObserver(
      observer = this,
      forKeyPath = "URL",
      options = NSKeyValueObservingOptionNew,
      context = null
    )
  }

  val urlFlow = MutableStateFlow("")

  override fun observeValueForKeyPath(
    keyPath: String?,
    ofObject: Any?,
    change: Map<Any?, *>?,
    context: COpaquePointer?,
  ) {
    if (keyPath == "URL") {
      val title = change?.get("new") as? NSString
      urlFlow.value = title.toString()
    }
  }

  fun disconnect() {
    engine.removeObserver(
      observer = this,
      forKeyPath = "URL",
      context = null
    )
  }
}