package org.dweb_browser.dwebview

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.*

@Suppress("DEPRECATION")
class DWebChromeClient : WebChromeClient() {
  private val extends = Extends<WebChromeClient>()
  fun addWebChromeClient(client: WebChromeClient, config: Extends.Config = Extends.Config()) =
    extends.add(client, config)

  fun removeWebChromeClient(client: WebChromeClient) = extends.remove(client)

  private fun inners(methodName: String, noise: Boolean = true) =
    extends.hasMethod(methodName)
      .also {
        if (it.isNotEmpty() && noise) {
          debugDWebView("WebChromeClient", "calling method: $methodName")
        }
      }


  override fun getDefaultVideoPoster(): Bitmap? {
    return inners("getDefaultVideoPoster").mapFindNoNull { it.getDefaultVideoPoster() }
      ?: super.getDefaultVideoPoster()
  }

  override fun getVideoLoadingProgressView(): View? {
    return inners("getVideoLoadingProgressView").mapFindNoNull { it.getVideoLoadingProgressView() }
      ?: super.getVideoLoadingProgressView()
  }

  override fun getVisitedHistory(callback: ValueCallback<Array<String>>?) {
    inners("getVisitedHistory").one { it.getVisitedHistory(callback) }
      ?: super.getVisitedHistory(callback)
  }

  override fun onCloseWindow(window: WebView?) {
    inners("onCloseWindow").one { it.onCloseWindow(window) } ?: super.onCloseWindow(window)
  }

  override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
    return inners("onConsoleMessage", false).mapFindNoNull { it.onConsoleMessage(consoleMessage) }
      ?: super.onConsoleMessage(
        consoleMessage
      )
  }

  override fun onCreateWindow(
    view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?
  ): Boolean {
    return inners("onCreateWindow").mapFindNoNull {
      it.onCreateWindow(
        view, isDialog, isUserGesture, resultMsg
      )
    } ?: super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
  }

  override fun onGeolocationPermissionsHidePrompt() {
    inners("onGeolocationPermissionsHidePrompt").one { it.onGeolocationPermissionsHidePrompt() }
      ?: super.onGeolocationPermissionsHidePrompt()
  }

  override fun onGeolocationPermissionsShowPrompt(
    origin: String?, callback: GeolocationPermissions.Callback?
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
    view: WebView?, url: String?, message: String?, result: JsResult?
  ): Boolean {
    return inners("onJsAlert").mapFindNoNull { it.onJsAlert(view, url, message, result) }
      ?: super.onJsAlert(
        view, url, message, result
      )
  }

  override fun onJsBeforeUnload(
    view: WebView?, url: String?, message: String?, result: JsResult?
  ): Boolean {
    return inners("onJsBeforeUnload").someOrNull { it.onJsBeforeUnload(view, url, message, result) }
      ?: super.onJsBeforeUnload(view, url, message, result)
  }

  override fun onJsConfirm(
    view: WebView?, url: String?, message: String?, result: JsResult?
  ): Boolean {
    return inners("onJsConfirm").mapFindNoNull { it.onJsConfirm(view, url, message, result) }
      ?: super.onJsConfirm(
        view, url, message, result
      )
  }

  override fun onJsPrompt(
    view: WebView?,
    url: String?,
    message: String?,
    defaultValue: String?,
    result: JsPromptResult?
  ): Boolean {
    return inners("onJsPrompt").mapFindNoNull { it.onJsPrompt(view, url, message, defaultValue, result) }
      ?: super.onJsPrompt(view, url, message, defaultValue, result)
  }

  override fun onPermissionRequest(request: PermissionRequest?) {
    inners("onPermissionRequest").one { it.onPermissionRequest(request) }
      ?: super.onPermissionRequest(request)
  }

  override fun onPermissionRequestCanceled(request: PermissionRequest?) {
    inners("onPermissionRequestCanceled").one { it.onPermissionRequestCanceled(request) }
      ?: super.onPermissionRequestCanceled(request)
  }

  override fun onProgressChanged(view: WebView?, newProgress: Int) {
    inners("onProgressChanged").one { it.onProgressChanged(view, newProgress) }
      ?: super.onProgressChanged(
        view, newProgress
      )
  }

  override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
    inners("onReceivedIcon").one { it.onReceivedIcon(view, icon) }
      ?: super.onReceivedIcon(view, icon)
  }

  override fun onReceivedTitle(view: WebView?, title: String?) {
    inners("onReceivedTitle").one { it.onReceivedTitle(view, title) } ?: super.onReceivedTitle(
      view, title
    )
  }

  override fun onReceivedTouchIconUrl(view: WebView?, url: String?, precomposed: Boolean) {
    inners("onReceivedTouchIconUrl").one { it.onReceivedTouchIconUrl(view, url, precomposed) }
      ?: super.onReceivedTouchIconUrl(view, url, precomposed)
  }

  override fun onRequestFocus(view: WebView?) {
    inners("onRequestFocus").one { it.onRequestFocus(view) } ?: super.onRequestFocus(view)
  }

  override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
    inners("onShowCustomView").one { it.onShowCustomView(view, callback) }
      ?: super.onShowCustomView(
        view, callback
      )
  }

  override fun onShowFileChooser(
    webView: WebView?,
    filePathCallback: ValueCallback<Array<Uri>>?,
    fileChooserParams: FileChooserParams?
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
        message,
        lineNumber,
        sourceID
      )
    }
      ?: super.onConsoleMessage(message, lineNumber, sourceID)
  }

  @Deprecated("Deprecated in Java")
  override fun onExceededDatabaseQuota(
    url: String?,
    databaseIdentifier: String?,
    quota: Long,
    estimatedDatabaseSize: Long,
    totalQuota: Long,
    quotaUpdater: WebStorage.QuotaUpdater?
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
    view: View?, requestedOrientation: Int, callback: CustomViewCallback?
  ) {
    inners("onShowCustomView").one {
      it.onShowCustomView(
        view, requestedOrientation, callback
      )
    } ?: super.onShowCustomView(view, requestedOrientation, callback)
  }
}