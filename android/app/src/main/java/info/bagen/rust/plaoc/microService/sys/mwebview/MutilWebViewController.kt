package info.bagen.rust.plaoc.microService.sys.mwebview

import android.webkit.WebView
import androidx.compose.runtime.mutableStateListOf
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.webview.DWebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MutilWebViewController(val mmid: Mmid) {

    companion object {
        private var webviewId_acc = 1
    }

    val webViewList = mutableStateListOf<ViewItem>()

    data class ViewItem(
        val webviewId: String,
        val dWebView: DWebView,
        var hidden: Boolean = false
    )


    /**
     * 打开WebView
     */
    @Synchronized
    fun openWebView(module: MicroModule, url: String): ViewItem {
        val webviewId = "#w${webviewId_acc++}"
        val dWebView = runBlocking(Dispatchers.Main) {
            val dWebView = DWebView(
                App.appContext,
                module,
                DWebView.Options(url = url)
            )
            dWebView.onOpen { message ->
                val dWebViewChild = openWebView(
                    module,
                    ""
                ).dWebView
                val transport = message.obj;
                if (transport is WebView.WebViewTransport) {
                    transport.webView = dWebViewChild;
                    message.sendToTarget();
                }
            }
            dWebView.onClose {
                closeWebView(webviewId)
            }

            dWebView
        }
        return ViewItem(webviewId, dWebView).also {
            webViewList.add(it)
        }
    }

    /**
     * 关闭WebView
     */
    @Synchronized
    fun closeWebView(webviewId: String): Boolean {
        return webViewList.removeIf {
            if (it.webviewId == webviewId) {
                it.dWebView.destroy()
                true
            } else {
                false
            }
        }
    }

    /**
     * 将指定WebView移动到顶部显示
     */
    fun moveToTopWebView(webviewId: String): Boolean {
        val viewItem = webViewList.find { it.webviewId == webviewId } ?: return false
        webViewList.remove(viewItem)
        webViewList.add(viewItem)
        return true
    }


}