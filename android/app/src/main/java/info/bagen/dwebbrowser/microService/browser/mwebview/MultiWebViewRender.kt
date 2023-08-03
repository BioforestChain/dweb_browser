package info.bagen.dwebbrowser.microService.browser.mwebview

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.accompanist.web.WebView
import kotlinx.coroutines.launch

@Composable
fun MultiWebViewController.Render(modifier: Modifier = Modifier) {
  val controller = this;
  Box(modifier) {
    var list by remember {
      mutableStateOf(webViewList.toList())
    }
    DisposableEffect(webViewList) {
      val off = webViewList.onChange {
        list = it.toList()
        return@onChange null;
      }
      onDispose {
        off(Unit)
      }
    }
    list.forEach { viewItem ->
      key(viewItem.webviewId) {
        val nativeUiController = viewItem.nativeUiController.effect()
        val state = viewItem.state
        val navigator = viewItem.navigator

        val chromeClient = remember {
          MultiWebViewChromeClient(
            controller, viewItem, isLastView(viewItem)
          )
        }

        BackHandler(true) {
          if (chromeClient.closeWatcherController.canClose) {
            viewItem.coroutineScope.launch {
              chromeClient.closeWatcherController.close()
            }
          } else if (navigator.canGoBack) {
            debugMultiWebView("NAV/${viewItem.webviewId}", "go back")
            navigator.navigateBack()
          } else {
            viewItem.coroutineScope.launch {
              closeWebView(viewItem.webviewId)
            }
          }
        }

        Box(
          modifier = Modifier.fillMaxSize()
        ) {
          val modifierPadding by nativeUiController.safeArea.outerAreaInsetsState
          WebView(
            state = state,
            navigator = navigator,
            modifier = Modifier
              .fillMaxSize()
              .focusRequester(nativeUiController.virtualKeyboard.focusRequester)
              .padding(modifierPadding.asPaddingValues()),
            factory = {
              // 修复 activity 已存在父级时导致的异常
              viewItem.webView.parent?.let { parentView ->
                (parentView as ViewGroup).removeAllViews()
              }
              viewItem.webView.setBackgroundColor(Color.Transparent.toArgb())
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