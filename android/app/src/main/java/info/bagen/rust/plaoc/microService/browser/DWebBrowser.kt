package info.bagen.rust.plaoc.microService.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.*
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView


private val enterAnimator = slideInHorizontally(
    animationSpec = tween(500),//动画时长1s
    initialOffsetX = {
        it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
    }
)
private val exitAnimator = slideOutHorizontally(
    animationSpec = tween(500),//动画时长1s
    targetOffsetX = {
        it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
    }
)


@Composable
fun MultiDWebBrowserView(dWebBrowserModel: DWebBrowserModel) {

    AnimatedVisibility(
        visible = dWebBrowserModel.uiState.show.value, enter = enterAnimator, exit = exitAnimator
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            for (item in dWebBrowserModel.uiState.dWebBrowserList) {
                DWebBrowserView(dWebBrowserModel, item)
            }
        }
    }
}

@Composable
fun DWebBrowserView(dWebBrowserModel: DWebBrowserModel, item: DWebBrowserItem) {
    AnimatedVisibility(
        visible = item.show.value, enter = enterAnimator, exit = exitAnimator
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
        ) {
            BackHandler { dWebBrowserModel.handleIntent(DWebBrowserIntent.RemoveLast) }
            AndroidView(factory = {
                item.dWebBrowser
            })
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
class DWebBrowser(context: Context, url: String) : FrameLayout(context) {
    var mWebView: WebView
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