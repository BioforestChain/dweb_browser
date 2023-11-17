package info.bagen.dwebbrowser.microService.browser.mwebview

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.accompanist.web.WebView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.window.render.LocalWindowController
import org.dweb_browser.window.render.watchedIsMaximized
import java.util.concurrent.atomic.AtomicInteger

private var atomic: AtomicInteger = AtomicInteger(1)
private fun getNextInt(): Int {
  return if (atomic.toInt() == 2) {
    atomic.decrementAndGet()
  } else {
    atomic.incrementAndGet()
  }
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MultiWebViewController.Render(modifier: Modifier = Modifier, initialScale: Int) {
  val controller = this;
  val win = LocalWindowController.current
  Box(modifier) {
    var list by remember {
      mutableStateOf(webViewList.toList())
    }
    DisposableEffect(webViewList) {
      val off = webViewList.onChange {
        list = it.toList()
        if (list.isEmpty()) {
          win.close()
        }
        return@onChange;
      }
      onDispose {
        off()
      }
    }
    list.forEach { viewItem ->
      key(viewItem.webviewId) {
//        val nativeUiController = viewItem.nativeUiController.effect()
        val state = viewItem.state
        val navigator = viewItem.navigator
        var winRefresh by remember(initialScale) { mutableStateOf(false) }
        val chromeClient = remember {
          MultiWebViewChromeClient(controller, viewItem, isLastView(viewItem))
        }
        DisposableEffect(initialScale) {
          val off = viewItem.onReadyListener {
            winRefresh = true
          }
          onDispose {
            off()
          }
        }
        LaunchedEffect(initialScale) {
          snapshotFlow { winRefresh }.collect {
            if (winRefresh) {
              delay(100)
              viewItem.webView.setInitialScale(initialScale - getNextInt())
              winRefresh = false
            }
          }
        }

        /// 返回按钮的拦截只跟最后一个视图有关系，直到这最后一个视图被关闭了
        if (viewItem == list.last()) {
          val isMaximized by win.watchedIsMaximized()
          // 在 MWebView 的全屏窗口中，默认将返回按钮的行为与应用退出关联在一起
          if (win.state.canGoForward != null) {
            win.state.canGoForward = navigator.canGoForward
          }
          val canGoBack =
            if (isMaximized) true else chromeClient.closeWatcherController.canClose || navigator.canGoBack
          win.GoBackHandler(canGoBack) {
            if (navigator.canGoBack) {
              debugMultiWebView("NAV/${viewItem.webviewId}", "go back")
              navigator.navigateBack()
            } else if (chromeClient.closeWatcherController.canClose) {
              viewItem.coroutineScope.launch {
                chromeClient.closeWatcherController.close()
              }
            } else if (list.size > 1) {
              viewItem.coroutineScope.launch {
                closeWebView(viewItem.webviewId)
              }
            }
          }
        }

        Box(
          modifier = Modifier.fillMaxSize()
        ) {
          SideEffect {
            viewItem.webView.setInitialScale(initialScale)
          }

//          val modifierPadding by nativeUiController.safeArea.outerAreaInsetsState
          WebView(
            state = state,
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
//              .focusRequester(nativeUiController.virtualKeyboard.focusRequester)
//              .padding(modifierPadding.asPaddingValues())
            factory = {
              // 修复 activity 已存在父级时导致的异常
              viewItem.webView.parent?.let { parentView ->
                (parentView as ViewGroup).removeAllViews()
              }
              viewItem.webView.setBackgroundColor(Color.Transparent.toArgb())
//            viewItem.webView.activity.resources.obtainAttributes() androidx.appcompat.R.attr.colorAccent
//            viewItem.webView.resources.newTheme().obtainStyledAttributes(textColorHighlight )
              viewItem.webView
            },
            chromeClient = chromeClient,
            captureBackPresses = false,
          )
          chromeClient.BeforeUnloadDialog()
        }
      }
    }

  }

}