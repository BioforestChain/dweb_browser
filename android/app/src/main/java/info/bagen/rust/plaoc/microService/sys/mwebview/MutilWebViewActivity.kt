package info.bagen.rust.plaoc.microService.sys.mwebview

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.microService.core.MicroModule
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.webview.DWebView
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class MutilWebViewActivity : AppCompatActivity() {
    private val webViewList = mutableStateListOf<ViewItem>()

    private lateinit var remoteMmid: Mmid

    companion object {
        private var webviewId_acc = 1
    }

    //    val dWebBrowserModel
    data class ViewItem(
        val webviewId: String,
        val dWebView: DWebView,
        var hidden: Boolean = false
    ) {

    }

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
                DWebView.Options(loadUrl = url)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        remoteMmid = intent.getStringExtra("mmid")
            ?: throw Exception("No found remoteMmid for mutil-webview")

        MultiWebViewNMM.activityMap[remoteMmid]?.also { taskPo ->
            taskPo.resolve(this)
        } ?: return finish()

        setContent {
            RustApplicationTheme {
                val viewItem = webViewList.lastOrNull()
                if (viewItem != null) key(viewItem.webviewId) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                viewItem.dWebView
                            },
                            modifier = Modifier
                                .fillMaxSize(),
                        )
                    }
                }

            }

        }
    }


}