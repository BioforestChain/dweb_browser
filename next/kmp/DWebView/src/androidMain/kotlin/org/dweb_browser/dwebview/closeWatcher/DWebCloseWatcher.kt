package org.dweb_browser.dwebview.closeWatcher

import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.withMainContext

class DWebCloseWatcher(private val engine: DWebViewEngine) : WebChromeClient() {
  override fun onCreateWindow(
    view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message
  ): Boolean {
    val transport = resultMsg.obj;
    if (transport is WebView.WebViewTransport) {
      engine.ioScope.launch {
        val dwebView =
          DWebViewEngine(engine.context, engine.remoteMM, DWebViewOptions(), engine.activity)
        transport.webView = dwebView
        resultMsg.sendToTarget()

        // 它是有内部链接的，所以等到它ok了再说
        var mainUrl = dwebView.getUrlInMain()
        if (mainUrl?.isEmpty() != true) {
          dwebView.waitReady()
          mainUrl = dwebView.getUrlInMain()
        }

        /// 内部特殊行为，有时候，我们需要知道 isUserGesture 这个属性，所以需要借助 onCreateWindow 这个回调来实现
        /// 实现 CloseWatcher 提案 https://github.com/WICG/close-watcher/blob/main/README.md
        if (engine.closeWatcher.consuming.remove(mainUrl)) {
          val consumeToken = mainUrl!!
          engine.closeWatcher.apply(isUserGesture).also {
            withMainContext {
              dwebView.destroy()
              engine.closeWatcher.resolveToken(consumeToken, it)
            }
          }
        } else {
          /// 打开一个新窗口
          engine.createWindowSignal.emit(IDWebView.create(dwebView, mainUrl))
        }
      }
      return true
    }
    return super.onCreateWindow(
      view, isDialog, isUserGesture, resultMsg
    )
  }
}