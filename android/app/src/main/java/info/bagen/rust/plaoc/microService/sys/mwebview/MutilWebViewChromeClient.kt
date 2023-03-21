package info.bagen.rust.plaoc.microService.sys.mwebview

import android.os.Message
import android.webkit.JsResult
import android.webkit.WebView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.accompanist.web.AccompanistWebChromeClient
import info.bagen.rust.plaoc.microService.sys.mwebview.CloseWatcher.CloseWatcher
import kotlinx.coroutines.launch

class MutilWebViewChromeClient(
    val wc: MultiWebViewController,
    val viewItem: MultiWebViewController.ViewItem,
    val isLast: Boolean
) : AccompanistWebChromeClient() {
//    val viewItem = wc.currentView
//    val isLast = wc.currentIsLast

    //#region BeforeUnload
    val beforeUnloadController = BeforeUnloadController()

    override fun onJsBeforeUnload(
        view: WebView, url: String, message: String, result: JsResult
    ): Boolean {
        debugMultiWebView(
            "onJsBeforeUnload", "url:$url message:$message"
        )
        if (message.isNotEmpty()) {
            if (isLast) {
                beforeUnloadController.promptState.value = message
                beforeUnloadController.resultState.value = result
            } else {
                result.cancel()
            }
            return true
        }
        return super.onJsBeforeUnload(view, url, message, result)
    }


    class BeforeUnloadController {
        val promptState = mutableStateOf("")
        val resultState = mutableStateOf<JsResult?>(null)
    }

    @Composable
    fun beforeUnloadDialog() {

        var beforeUnloadPrompt by beforeUnloadController.promptState
        var beforeUnloadResult by beforeUnloadController.resultState
        val jsResult = beforeUnloadResult ?: return

        AlertDialog(
            title = {
                Text("确定要离开吗？")// TODO i18n
            },
            text = { Text(beforeUnloadPrompt) },
            onDismissRequest = {
                jsResult.cancel()
                beforeUnloadResult = null
            },
            confirmButton = {
                Button(onClick = {
                    jsResult.confirm()
                    beforeUnloadResult = null
                    wc.closeWebView(viewItem.webviewId)
                }) {
                    Text("确定")// TODO i18n
                }
            },
            dismissButton = {
                Button(onClick = {
                    jsResult.cancel()
                    beforeUnloadResult = null
                }) {
                    Text("留下")// TODO i18n
                }
            })
    }

    //#endregion

    //#region NewWindow & CloseWatcher

    val closeWatcherController = CloseWatcher(viewItem)

    override fun onCreateWindow(
        view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
    ): Boolean {
        val transport = resultMsg.obj;
        if (transport is WebView.WebViewTransport) {
            viewItem.coroutineScope.launch {
                debugMultiWebView("opening")
                val dWebView = wc.createDwebView("")
                transport.webView = dWebView;
                resultMsg.sendToTarget();

                // 它是有内部链接的，所以等到它ok了再说
                var url = dWebView.getUrlInMain()
                if (url?.isEmpty() != true) {
                    dWebView.waitReady()
                    url = dWebView.getUrlInMain()
                }
                debugMultiWebView("opened", url)

                val DWEB_INTERNAL_URL_PREFIX = "data:text/html,dweb-internal/"
                /// 内部特殊行为，有时候，我们需要知道 isUserGesture 这个属性，所以需要借助 onCreateWindow 这个回调来实现
                if (url?.startsWith(DWEB_INTERNAL_URL_PREFIX) == true) {
                    val url = url.substring(DWEB_INTERNAL_URL_PREFIX.length);
                    dWebView.destroy() // 这种情况下，webview只是一个用于获取url的工具而已，所以可以直接销毁掉
                    /// 实现 CloseWatcher 提案 https://github.com/WICG/close-watcher/blob/main/README.md
                    if (url.startsWith(CloseWatcher.CREATE_CLOSE_WATCHER_PREFIX)) {
                        val consumeToken =
                            url.substring(CloseWatcher.CREATE_CLOSE_WATCHER_PREFIX.length)
                        if (closeWatcherController.consuming.remove(consumeToken)) {
                            closeWatcherController.apply(isUserGesture).also {
                                closeWatcherController.resolveToken(consumeToken, it)
                            }

                        }
                    }
                } else {
                    /// 打开一个新窗口
                    wc.appendWebViewAsItem(dWebView)
                }
            }
            return true
        }

        return super.onCreateWindow(
            view, isDialog, isUserGesture, resultMsg
        )
    }


    //#endregion
}