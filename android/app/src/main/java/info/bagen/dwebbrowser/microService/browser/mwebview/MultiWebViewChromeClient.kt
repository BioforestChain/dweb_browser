package info.bagen.dwebbrowser.microService.browser.mwebview

import android.os.Message
import android.webkit.JsResult
import android.webkit.WebView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.google.accompanist.web.AccompanistWebChromeClient
import org.dweb_browser.helper.*
import org.dweb_browser.dwebview.closeWatcher.CloseWatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MultiWebViewChromeClient(
  val wc: MultiWebViewController,
  val viewItem: MultiWebViewController.MultiViewItem,
  val isLast: Boolean
) : AccompanistWebChromeClient() {
//    val viewItem = wc.currentView
//    val isLast = wc.currentIsLast

  //#region BeforeUnload
  val beforeUnloadController = BeforeUnloadController()

  override fun onJsBeforeUnload(
    view: WebView, url: String, message: String, result: JsResult
  ): Boolean {
    debugMultiWebView(
      "onJsBeforeUnload", "url:$url message:$message"
    )
    if (message.isNotEmpty()) {
      if (isLast) {
        beforeUnloadController.promptState.value = message
        beforeUnloadController.resultState.value = result
      } else {
        result.cancel()
      }
      return true
    }
    return super.onJsBeforeUnload(view, url, message, result)
  }


  class BeforeUnloadController {
    val promptState = mutableStateOf("")
    val resultState = mutableStateOf<JsResult?>(null)
  }

  @Composable
  fun beforeUnloadDialog() {

    var beforeUnloadPrompt by beforeUnloadController.promptState
    var beforeUnloadResult by beforeUnloadController.resultState
    val jsResult = beforeUnloadResult ?: return

    val sc = rememberCoroutineScope()

    AlertDialog(
      title = {
        Text("确定要离开吗？")// TODO i18n
      },
      text = { Text(beforeUnloadPrompt) },
      onDismissRequest = {
        jsResult.cancel()
        beforeUnloadResult = null
      },
      confirmButton = {
        Button(onClick = {
          jsResult.confirm()
          beforeUnloadResult = null
          sc.launch {
            wc.closeWebView(viewItem.webviewId)
          }
        }) {
          Text("确定")// TODO i18n
        }
      },
      dismissButton = {
        Button(onClick = {
          jsResult.cancel()
          beforeUnloadResult = null
        }) {
          Text("留下")// TODO i18n
        }
      })
  }

  //#endregion

  //#region NewWindow & CloseWatcher

  val closeWatcherController = CloseWatcher(viewItem)

  override fun onCreateWindow(
    view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
  ): Boolean {
    val transport = resultMsg.obj;
    if (transport is WebView.WebViewTransport) {
      viewItem.coroutineScope.launch {
        debugMultiWebView("opening")
        val dWebView = wc.createDwebView("")
        transport.webView = dWebView;
        resultMsg.sendToTarget();

        // 它是有内部链接的，所以等到它ok了再说
        var url = dWebView.getUrlInMain()
        if (url?.isEmpty() != true) {
          dWebView.waitReady()
          url = dWebView.getUrlInMain()
        }
        debugMultiWebView("opened", url)

        /// 内部特殊行为，有时候，我们需要知道 isUserGesture 这个属性，所以需要借助 onCreateWindow 这个回调来实现
        /// 实现 CloseWatcher 提案 https://github.com/WICG/close-watcher/blob/main/README.md
        if (closeWatcherController.consuming.remove(url)) {
          val consumeToken = url!!
          closeWatcherController.apply(isUserGesture).also {
            withContext(mainAsyncExceptionHandler) {
              dWebView.destroy()
              closeWatcherController.resolveToken(consumeToken, it)
            }
          }
        } else {
          /// 打开一个新窗口
          wc.appendWebViewAsItem(dWebView)
        }
      }
      return true
    }

    return super.onCreateWindow(
      view, isDialog, isUserGesture, resultMsg
    )
  }


  //#endregion
}