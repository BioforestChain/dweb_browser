package info.bagen.dwebbrowser.microService.mwebview

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import com.google.accompanist.web.WebView
import kotlinx.coroutines.launch
import org.dweb_browser.helper.Mmid

@Composable
fun MultiWebView(mmid: Mmid, viewItem: MultiWebViewController.MultiViewItem) {
  key(viewItem.webviewId) {
    val wc = MultiWebViewNMM.getCurrentWebViewController(mmid) ?: return@key
    val nativeUiController = viewItem.nativeUiController.effect()
    val state = viewItem.state
    val navigator = viewItem.navigator

    val chromeClient = remember { MultiWebViewChromeClient(wc, viewItem, wc.isLastView(viewItem)) }

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
          wc.closeWebView(viewItem.webviewId)
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
          viewItem.webView
        },
        chromeClient = chromeClient,
        captureBackPresses = false,
      )
      chromeClient.BeforeUnloadDialog()
    }
  }
}