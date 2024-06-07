package org.dweb_browser.browser.common

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.helper.platform.IPureViewController

expect suspend fun IPureViewController.createDwebView(
  remoteMM: MicroModule.Runtime,
  options: DWebViewOptions,
): IDWebView
