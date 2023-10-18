package org.dweb_browser.browser.mwebview

import kotlinx.coroutines.withContext
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.sys.window.core.WindowController

suspend fun WindowController.createDwebView(remoteMM: MicroModule, url: String): DWebViewEngine =
  withContext(mainAsyncExceptionHandler) {
    val currentActivity = viewController.activity!!;// App.appContext
    val dWebView = DWebViewEngine(
      currentActivity, remoteMM, DWebViewOptions(
        url = url,
        /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
        onDetachedFromWindowStrategy = DWebViewOptions.DetachedFromWindowStrategy.Ignore,
      ), currentActivity
    )
    dWebView
  }