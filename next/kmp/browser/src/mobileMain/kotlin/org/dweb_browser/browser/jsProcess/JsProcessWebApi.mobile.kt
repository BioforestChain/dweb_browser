package org.dweb_browser.browser.jsProcess

import kotlinx.coroutines.CoroutineScope
import org.dweb_browser.dwebview.IDWebView

// 移动端不需要实现
actual fun listenOpenDevTool(dWebView: IDWebView, scope: CoroutineScope) {
}