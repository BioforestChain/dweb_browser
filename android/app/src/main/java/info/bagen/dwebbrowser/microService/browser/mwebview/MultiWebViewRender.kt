package info.bagen.dwebbrowser.microService.browser.mwebview

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.web.WebView
import info.bagen.dwebbrowser.base.BaseActivity
import org.dweb_browser.helper.*
import info.bagen.dwebbrowser.microService.browser.mwebviewbak.MultiWebViewNMM.Companion.getCurrentWebViewController
import info.bagen.dwebbrowser.microService.browser.mwebviewbak.dwebServiceWorker.ServiceWorkerEvent
import info.bagen.dwebbrowser.microService.browser.mwebviewbak.dwebServiceWorker.emitEvent
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme
import kotlinx.coroutines.launch

@Composable
fun MultiWebViewController.Render(modifier: Modifier = Modifier) {
  val controller = this;
  Box(modifier) {
    webViewList.forEach { viewItem ->
      key(viewItem.webviewId) {
        val nativeUiController = viewItem.nativeUiController.effect()
        val state = viewItem.state
        val navigator = viewItem.navigator

        val chromeClient = remember {
          MultiWebViewChromeClient(
            controller,
            viewItem,
            isLastView(viewItem)
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