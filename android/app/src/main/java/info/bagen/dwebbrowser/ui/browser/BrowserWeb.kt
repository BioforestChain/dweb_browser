package info.bagen.dwebbrowser.ui.browser

import android.annotation.SuppressLint
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import com.google.accompanist.web.LoadingState
import com.google.accompanist.web.WebView
import info.bagen.dwebbrowser.ui.entity.BrowserWebView
import info.bagen.dwebbrowser.ui.view.drawToBitmapPostLaidOut
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

@SuppressLint("ClickableViewAccessibility")
@Composable
internal fun BrowserWebView(viewModel: BrowserViewModel, browserWebView: BrowserWebView) {
    val localFocusManager = LocalFocusManager.current
    val webViewY = 0 // 用于截图的时候进行定位截图
    LaunchedEffect(browserWebView.state) { // 点击跳转时，加载状态变化，将底部栏显示
        snapshotFlow { browserWebView.state.loadingState }.collect {
            if (it is LoadingState.Loading) {
                viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(true))
            }
            if (it is LoadingState.Finished) {
                delay(500)
                viewModel.handleIntent(
                    BrowserIntent.SaveHistoryWebSiteInfo(
                        browserWebView.state.pageTitle, browserWebView.state.lastLoadedUrl
                    )
                )
                browserWebView.controller.capture()
            }
        }
    }

    LaunchedEffect(browserWebView.controller) {
        browserWebView.controller.captureRequests.mapNotNull {
            delay(500)
            browserWebView.webView.drawToBitmapPostLaidOut(webViewY)
        }.onEach {
            it.first?.let { bitmap ->
                browserWebView.bitmap = bitmap.asImageBitmap()
            }
        }.launchIn(this)
    }

    val background = MaterialTheme.colorScheme.background
    val isDark = isSystemInDarkTheme()
    //val webViewClient = BrowserWebViewClient()
    WebView(
        state = browserWebView.state,
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        navigator = browserWebView.navigator,
        factory = {
            browserWebView.webView.parent?.let { (it as ViewGroup).removeAllViews() }
           // browserWebView.webView.webViewClient = webViewClient
            browserWebView.webView.apply {
                setDarkMode(isDark, background) // 设置深色主题
                setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        browserWebView.controller.capture()
                    }
                    false
                }
                // 滚动隐藏先屏蔽
                /*setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                  webViewY = scrollY // 用于截图的时候进行定位截图
                  if (scrollY == 0 || oldScrollY == 0) return@setOnScrollChangeListener
                  localFocusManager.clearFocus() // TODO 清除焦点
                  if (oldScrollY < scrollY - 5) {
                    viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(false)) // TODO 上滑，需要隐藏底部栏
                  } else if (oldScrollY > scrollY + 5) {
                    viewModel.handleIntent(BrowserIntent.UpdateBottomViewState(true))  // TODO 下滑，需要显示底部栏
                  }
                }*/
            }
            browserWebView.webView
        })
}

/**
 * 用于设置当前的 WebView 是否跟随系统深色主题
 * @param isDark 是否是深色主题
 */
fun WebView.setDarkMode(isDark: Boolean, background: Color) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        settings.isAlgorithmicDarkeningAllowed = isDark
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        settings.forceDark = if (isDark) WebSettings.FORCE_DARK_ON else WebSettings.FORCE_DARK_OFF
    } else {
        setBackgroundColor(background.value.toInt())
    }
}
