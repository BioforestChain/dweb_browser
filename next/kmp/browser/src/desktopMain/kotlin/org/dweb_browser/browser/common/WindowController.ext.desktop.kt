package org.dweb_browser.browser.common

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.sys.window.core.WindowController

actual suspend fun WindowController.createDwebView(remoteMM: MicroModule.Runtime, url: String) =
  IDWebView.create(
    remoteMM,
    DWebViewOptions(
      url = url,
      /// 我们会完全控制页面将如何离开，所以这里兜底默认为留在页面
      detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
    )
  )
