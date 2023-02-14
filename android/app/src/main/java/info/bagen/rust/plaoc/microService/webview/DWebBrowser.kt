package info.bagen.rust.plaoc.microService.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MultiDWebBrowserView(dWebBrowserModel: DWebBrowserModel) {
    for (item in dWebBrowserModel.uiState.dWebBrowserList) {
        AnimatedVisibility(
            visible = item.show.value,
            enter = slideInHorizontally(),
            exit = slideOutHorizontally()
        ) {
            Box(modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .animateEnterExit(
                    enter = slideInHorizontally(),
                    exit = slideOutHorizontally()
                )) {
                BackHandler { dWebBrowserModel.handleIntent(DWebBrowserIntent.RemoveLast) }
                AndroidView(factory = { item.dWebBrowser })
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
class DWebBrowser(context: Context, url: String) : FrameLayout(context) {
    private var mWebView: WebView

    init {
        this.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        this.mWebView = WebView(context).apply {
            this.settings.apply {
                javaScriptEnabled = true
                allowFileAccess = true
                domStorageEnabled = true
            }

            this.webViewClient = DWebBrowserViewClient()
            this.webChromeClient = DWebBrowserChromeClient()
            loadUrl(url)
        }

        this.addView(this.mWebView)
    }

    fun canGoBack() = mWebView.canGoBack()
    fun goBack() = mWebView.goBack()
    fun destroy() = mWebView.destroy()
}