package org.dweb_browser.browser.jsProcess

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.platform.LocalViewHookJsProcess

actual fun listenOpenDevTool(dWebView: IDWebView, scope: CoroutineScope) {
  scope.launch {
    LocalViewHookJsProcess.flow().collect {
      dWebView.openDevTool()
    }
  }
}