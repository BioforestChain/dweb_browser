package info.bagen.rust.plaoc.microService.sys.mwebview

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateMapOf
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
    private val webViewList = mutableStateMapOf<String, DWebView>()

    private lateinit var remoteMmid: Mmid

    companion object {
        private var webviewId_acc = 1
    }

    //    val dWebBrowserModel
    data class OpenResult(val webviewId: String, val dWebView: DWebView)

    fun openWebView(module: MicroModule, url: String): OpenResult {
        val webviewId = "#w${webviewId_acc++}"
        val dWebView = runBlocking(Dispatchers.Main) {
            val dWebView = DWebView(
                App.appContext,
                module,
                DWebView.Options(loadUrl = url)
            )
            webViewList[webviewId] = dWebView
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
        return OpenResult(webviewId, dWebView)
    }

    fun closeWebView(webviewId: String): Boolean {
        return webViewList.remove(webviewId)?.also {
            it.destroy()
        } != null
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
                for (viewEntry in webViewList) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                viewEntry.value
                            },
                            modifier = Modifier
                                .fillMaxSize()
                        )
                    }
                }
            }

        }
    }
}