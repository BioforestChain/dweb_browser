package info.bagen.rust.plaoc.microService.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MultiDWebBrowserView(dWebBrowserModel: DWebBrowserModel) {
    for (item in dWebBrowserModel.uiState.dWebBrowserList) {
        AnimatedVisibility(visible = item.show.value) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(factory = { item.dWebBrowser })
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
class DWebBrowser(context: Context, url: String) : WebView(context) {

    init {
        this.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = true
            domStorageEnabled = true
        }

        this.webViewClient = DWebBrowserViewClient()
        this.webChromeClient = DWebBrowserChromeClient()
    }
}