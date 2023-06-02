package info.bagen.dwebbrowser.microService.browser.webview

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.*

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
        return inners("getDefaultVideoPoster").lets { it.getDefaultVideoPoster() }
            ?: super.getDefaultVideoPoster()
    }

    override fun getVideoLoadingProgressView(): View? {
        return inners("getVideoLoadingProgressView").lets { it.getVideoLoadingProgressView() }
            ?: super.getVideoLoadingProgressView()
    }

    override fun getVisitedHistory(callback: ValueCallback<Array<String>>?) {
        inners("getVisitedHistory").some { it.getVisitedHistory(callback) }
            ?: super.getVisitedHistory(callback)
    }

    override fun onCloseWindow(window: WebView?) {
        inners("onCloseWindow").some { it.onCloseWindow(window) } ?: super.onCloseWindow(window)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        return inners("onConsoleMessage", false).lets { it.onConsoleMessage(consoleMessage) }
            ?: super.onConsoleMessage(
                consoleMessage
            )
    }

    override fun onCreateWindow(
        view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?
    ): Boolean {
        return inners("onCreateWindow").lets {
            it.onCreateWindow(
                view, isDialog, isUserGesture, resultMsg
            )
        } ?: super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
    }

    override fun onGeolocationPermissionsHidePrompt() {
        inners("onGeolocationPermissionsHidePrompt").some { it.onGeolocationPermissionsHidePrompt() }
            ?: super.onGeolocationPermissionsHidePrompt()
    }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String?, callback: GeolocationPermissions.Callback?
    ) {
        inners("onGeolocationPermissionsShowPrompt").some {
            it.onGeolocationPermissionsShowPrompt(
                origin, callback
            )
        } ?: super.onGeolocationPermissionsShowPrompt(origin, callback)
    }

    override fun onHideCustomView() {
        inners("onHideCustomView").some { it.onHideCustomView() } ?: super.onHideCustomView()
    }

    override fun onJsAlert(
        view: WebView?, url: String?, message: String?, result: JsResult?
    ): Boolean {
        return inners("onJsAlert").lets { it.onJsAlert(view, url, message, result) }
            ?: super.onJsAlert(
                view, url, message, result
            )
    }

    override fun onJsBeforeUnload(
        view: WebView?, url: String?, message: String?, result: JsResult?
    ): Boolean {
        return inners("onJsBeforeUnload").until { it.onJsBeforeUnload(view, url, message, result) }
            ?: super.onJsBeforeUnload(view, url, message, result)
    }

    override fun onJsConfirm(
        view: WebView?, url: String?, message: String?, result: JsResult?
    ): Boolean {
        return inners("onJsConfirm").lets { it.onJsConfirm(view, url, message, result) }
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
        return inners("onJsPrompt").lets { it.onJsPrompt(view, url, message, defaultValue, result) }
            ?: super.onJsPrompt(view, url, message, defaultValue, result)
    }

    override fun onPermissionRequest(request: PermissionRequest?) {
        inners("onPermissionRequest").some { it.onPermissionRequest(request) }
            ?: super.onPermissionRequest(request)
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        inners("onPermissionRequestCanceled").some { it.onPermissionRequestCanceled(request) }
            ?: super.onPermissionRequestCanceled(request)
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        inners("onProgressChanged").some { it.onProgressChanged(view, newProgress) }
            ?: super.onProgressChanged(
                view, newProgress
            )
    }

    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
        inners("onReceivedIcon").some { it.onReceivedIcon(view, icon) }
            ?: super.onReceivedIcon(view, icon)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        inners("onReceivedTitle").some { it.onReceivedTitle(view, title) } ?: super.onReceivedTitle(
            view, title
        )
    }

    override fun onReceivedTouchIconUrl(view: WebView?, url: String?, precomposed: Boolean) {
        inners("onReceivedTouchIconUrl").some { it.onReceivedTouchIconUrl(view, url, precomposed) }
            ?: super.onReceivedTouchIconUrl(view, url, precomposed)
    }

    override fun onRequestFocus(view: WebView?) {
        inners("onRequestFocus").some { it.onRequestFocus(view) } ?: super.onRequestFocus(view)
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        inners("onShowCustomView").some { it.onShowCustomView(view, callback) }
            ?: super.onShowCustomView(
                view, callback
            )
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return inners("onShowFileChooser").lets {
            it.onShowFileChooser(
                webView, filePathCallback, fileChooserParams
            )
        } ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    override fun onConsoleMessage(message: String?, lineNumber: Int, sourceID: String?) {
        inners("onConsoleMessage", false).some {
            it.onConsoleMessage(
                message,
                lineNumber,
                sourceID
            )
        }
            ?: super.onConsoleMessage(message, lineNumber, sourceID)
    }

    override fun onExceededDatabaseQuota(
        url: String?,
        databaseIdentifier: String?,
        quota: Long,
        estimatedDatabaseSize: Long,
        totalQuota: Long,
        quotaUpdater: WebStorage.QuotaUpdater?
    ) {
        inners("onExceededDatabaseQuota").some {
            it.onExceededDatabaseQuota(
                url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater
            )
        } ?: super.onExceededDatabaseQuota(
            url, databaseIdentifier, quota, estimatedDatabaseSize, totalQuota, quotaUpdater
        )
    }

    override fun onJsTimeout(): Boolean {
        return inners("onJsTimeout").lets { it.onJsTimeout() } ?: super.onJsTimeout()
    }

    override fun onShowCustomView(
        view: View?, requestedOrientation: Int, callback: CustomViewCallback?
    ) {
        inners("onShowCustomView").some {
            it.onShowCustomView(
                view, requestedOrientation, callback
            )
        } ?: super.onShowCustomView(view, requestedOrientation, callback)
    }
}