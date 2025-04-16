package org.dweb_browser.dwebview.engine

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.WebBeforeUnloadArgs
import org.dweb_browser.dwebview.WebLoadSuccessState
import org.dweb_browser.dwebview.create
import org.dweb_browser.dwebview.debugDWebView
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.globalMainScope
import org.dweb_browser.helper.mapFindNoNull
import org.dweb_browser.helper.one
import org.dweb_browser.helper.some
import org.dweb_browser.helper.someOrNull

@Suppress("DEPRECATION")
class DWebChromeClient(val engine: DWebViewEngine) : WebChromeClient() {
  private val scope get() = engine.lifecycleScope
  private val extends = Extends<WebChromeClient>()
  fun addWebChromeClient(client: WebChromeClient, config: Extends.Config = Extends.Config()) =
    extends.add(client, config)

  fun removeWebChromeClient(client: WebChromeClient) = extends.remove(client)

  private fun inners(methodName: String, noise: Boolean = true) =
    extends.hasMethod(methodName).also {
      if (it.isNotEmpty() && noise) {
        debugDWebView("WebChromeClient") { "calling method: $methodName" }
      }
    }


  override fun getDefaultVideoPoster(): Bitmap? {
    return inners("getDefaultVideoPoster").mapFindNoNull { it.defaultVideoPoster }
      ?: super.getDefaultVideoPoster()
  }

  override fun getVideoLoadingProgressView(): View? {
    return inners("getVideoLoadingProgressView").mapFindNoNull { it.videoLoadingProgressView }
      ?: super.getVideoLoadingProgressView()
  }

  override fun getVisitedHistory(callback: ValueCallback<Array<String>>?) {
    inners("getVisitedHistory").one { it.getVisitedHistory(callback) } ?: super.getVisitedHistory(
      callback
    )
  }

  internal val closeSignal = SimpleSignal()

  override fun onCloseWindow(window: WebView?) {
    scope.launch(start = CoroutineStart.UNDISPATCHED) {
      closeSignal.emit()
    }
    inners("onCloseWindow").one { it.onCloseWindow(window) } ?: super.onCloseWindow(window)
  }

  override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {

    /// 默认的自定义处理
    if (debugDWebView.isEnable && envSwitch.isEnabled(ENV_SWITCH_KEY.DWEBVIEW_JS_CONSOLE)) {
      /// 如果启用默认日志
      val message = consoleMessage.message()
      val lineNumber = consoleMessage.lineNumber()
      val source = consoleMessage.sourceId()
      when (val level = consoleMessage.messageLevel()) {
        ConsoleMessage.MessageLevel.ERROR -> debugDWebView(
          "JsConsole/$level",
          message,
          "<$source:$lineNumber>",
        )

        ConsoleMessage.MessageLevel.WARNING -> debugDWebView(
          "JsConsole/$level",
          "$message  <$source:$lineNumber>"
        )

        else -> debugDWebView.verbose("JsConsole/$level", "$message  <$source:$lineNumber>")
      }
      return false
    }
    return inners("onConsoleMessage", false).mapFindNoNull { it.onConsoleMessage(consoleMessage) }
      ?: true
    // 如果调用 super.onConsoleMessage( consoleMessage )，会有默认的打印机制
  }


  override fun onCreateWindow(
    view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message,
  ): Boolean {
    val transport = resultMsg.obj;
    if (transport is WebView.WebViewTransport) {
      globalMainScope.launch {
        val dwebView =
          DWebViewEngine(engine.context, engine.remoteMM, DWebViewOptions(), engine.activity)
        transport.webView = dwebView
        resultMsg.sendToTarget()

        // 它是有内部链接的，所以等到它ok了再说
        // 这里用 shouldOverrideUrlLoading/onPageStarted 可能拦截不到的，所以最好的方式就是用定时器来轮询
        var mainUrl = dwebView.url
        try {
          while (mainUrl == null) {
            if (dwebView.destroyStateSignal.isDestroyed ||
              /**
               * destroyStateSignal.isDestroyed 可能不工作
               *
               * 这个 dwebView 是有堆栈的，url虽然延迟才能resolve出来，但是canGoBackOrForward一开始就是true
               * 如果它返回了false，那么根据chromium的源代码，说明它已经被destroy了
               */
              !dwebView.canGoBackOrForward(0)) {
              return@launch
            }
            delay(5)
            mainUrl = dwebView.url
          }
          // 初始化时url为空，在这边触发url变更，否则会打开一个about:blank页面
          dwebView.loadStateFlow.emit(WebLoadSuccessState(mainUrl))
        } catch (e: Throwable) {
          return@launch
        }

        val beforeCreateWindowEvent =
          DWebViewEngine.BeforeCreateWindow(dwebView, mainUrl, isUserGesture, isDialog)
        engine.beforeCreateWindow.emit(beforeCreateWindowEvent)
        if (!beforeCreateWindowEvent.isConsumed) {
          engine.createWindowSignal.emit(IDWebView.create(dwebView, mainUrl))
        }
      }
      return true
    }
    return inners("onCreateWindow").map {
      it.onCreateWindow(
        view, isDialog, isUserGesture, resultMsg
      )
    }.some { it }
  }

  override fun onGeolocationPermissionsHidePrompt() {
    inners("onGeolocationPermissionsHidePrompt").one { it.onGeolocationPermissionsHidePrompt() }
      ?: super.onGeolocationPermissionsHidePrompt()
  }

  override fun onGeolocationPermissionsShowPrompt(
    origin: String?, callback: GeolocationPermissions.Callback?,
  ) {
    inners("onGeolocationPermissionsShowPrompt").one {
      it.onGeolocationPermissionsShowPrompt(
        origin, callback
      )
    } ?: super.onGeolocationPermissionsShowPrompt(origin, callback)
  }

  override fun onHideCustomView() {
    inners("onHideCustomView").one { it.onHideCustomView() } ?: super.onHideCustomView()
  }

  override fun onJsAlert(
    view: WebView?, url: String?, message: String?, result: JsResult?,
  ): Boolean {
    return inners("onJsAlert").mapFindNoNull { it.onJsAlert(view, url, message, result) }
      ?: super.onJsAlert(
        view, url, message, result
      )
  }

  val beforeUnloadSignal = Signal<WebBeforeUnloadArgs>()

  override fun onJsBeforeUnload(
    view: WebView?, url: String?, message: String?, result: JsResult?,
  ): Boolean {
    if (message.isNullOrEmpty() && beforeUnloadSignal.isNotEmpty() && result != null) {
      val args = WebBeforeUnloadArgs(message!!)
      scope.launch {
        beforeUnloadSignal.emit(args)
        val leave = args.waitHookResults()
        if (leave) {
          result.confirm()
        } else {
          result.cancel()
          engine.loadStateFlow.emit(
            WebLoadSuccessState(engine.url ?: "about:blank")
          )
        }
      }

      /// 默认对话框不会显示
      return true
    }
    return inners("onJsBeforeUnload").someOrNull {
      it.onJsBeforeUnload(
        view, url, message, result
      )
    } ?: super.onJsBeforeUnload(view, url, message, result)
  }

  override fun onJsConfirm(
    view: WebView?, url: String?, message: String?, result: JsResult?,
  ): Boolean {
    return inners("onJsConfirm").mapFindNoNull { it.onJsConfirm(view, url, message, result) }
      ?: super.onJsConfirm(
        view, url, message, result
      )
  }

  override fun onJsPrompt(
    view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?,
  ): Boolean {
    return inners("onJsPrompt").mapFindNoNull {
      it.onJsPrompt(
        view, url, message, defaultValue, result
      )
    } ?: super.onJsPrompt(view, url, message, defaultValue, result)
  }

  override fun onPermissionRequest(request: PermissionRequest?) {
    inners("onPermissionRequest").one { it.onPermissionRequest(request) }
      ?: super.onPermissionRequest(request)
  }

  override fun onPermissionRequestCanceled(request: PermissionRequest?) {
    inners("onPermissionRequestCanceled").one { it.onPermissionRequestCanceled(request) }
      ?: super.onPermissionRequestCanceled(request)
  }

  val loadingProgressStateFlow = MutableStateFlow(1f)

  override fun onProgressChanged(view: WebView, newProgress: Int) {
    if (loadingProgressStateFlow.subscriptionCount.value > 0) {
      scope.launch {
        loadingProgressStateFlow.emit(newProgress / 100f)
      }
    }
    inners("onProgressChanged").forEach { it.onProgressChanged(view, newProgress) }
    super.onProgressChanged(view, newProgress)
  }

  override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
    inners("onReceivedIcon").forEach { it.onReceivedIcon(view, icon) }
    super.onReceivedIcon(view, icon)
  }

  override fun onReceivedTitle(view: WebView, title: String?) {
    inners("onReceivedTitle").forEach { it.onReceivedTitle(view, title) }
    super.onReceivedTitle(view, title)
  }

  override fun onReceivedTouchIconUrl(view: WebView?, url: String?, precomposed: Boolean) {
    inners("onReceivedTouchIconUrl").forEach { it.onReceivedTouchIconUrl(view, url, precomposed) }
    super.onReceivedTouchIconUrl(view, url, precomposed)
  }

  override fun onRequestFocus(view: WebView?) {
    inners("onRequestFocus").forEach { it.onRequestFocus(view) }
    super.onRequestFocus(view)
  }

  override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
    inners("onShowCustomView").forEach { it.onShowCustomView(view, callback) }
    super.onShowCustomView(
      view, callback
    )
  }

  override fun onShowFileChooser(
    webView: WebView?,
    filePathCallback: ValueCallback<Array<Uri>>?,
    fileChooserParams: FileChooserParams?,
  ): Boolean {
    return inners("onShowFileChooser").mapFindNoNull {
      it.onShowFileChooser(
        webView, filePathCallback, fileChooserParams
      )
    } ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
  }

  @Deprecated("Deprecated in Java")
  override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
    inners("onConsoleMessage", false).one {
      it.onConsoleMessage(
        message, lineNumber, sourceID
      )
    } ?: super.onConsoleMessage(message, lineNumber, sourceID)
  }

  @Deprecated("Deprecated in Java")
  override fun onExceededDatabaseQuota(
    url: String?,
    databaseIdentifier: String?,
    quota: Long,
    estimatedDatabaseSize: Long,
    totalQuota: Long,
    quotaUpdater: WebStorage.QuotaUpdater?,
  ) {
    inners("onExceededDatabaseQuota").one {
      it.onExceededDatabaseQuota(
        url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater
      )
    } ?: super.onExceededDatabaseQuota(
      url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater
    )
  }

  @Deprecated("Deprecated in Java")
  override fun onJsTimeout(): Boolean {
    return inners("onJsTimeout").mapFindNoNull { it.onJsTimeout() } ?: super.onJsTimeout()
  }

  @Deprecated("Deprecated in Java")
  override fun onShowCustomView(
    view: View?, requestedOrientation: Int, callback: CustomViewCallback?,
  ) {
    inners("onShowCustomView").one {
      it.onShowCustomView(
        view, requestedOrientation, callback
      )
    } ?: super.onShowCustomView(view, requestedOrientation, callback)
  }
}