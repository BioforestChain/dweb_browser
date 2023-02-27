package info.bagen.rust.plaoc.microService.sys.mwebview

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
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
    fun openWebView(module: MicroModule, url: String, dwebHost: String? = null): String {
        val webviewId = "#w${webviewId_acc++}"
        runBlocking(Dispatchers.Main) {
            webViewList[webviewId] =
                DWebView(
                    App.appContext,
                    module,
                    DWebView.Options(dwebHost = dwebHost ?: "", loadUrl = url)
                )
        }
        return url
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
                            .background(MaterialTheme.colors.primary)
                    ) {
                        AndroidView(factory = { ctx ->
                            viewEntry.value
                        })
                    }
                }
            }

        }
    }
}