package org.dweb_browser.browser.common

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.PureViewController

actual suspend fun IPureViewController.createDwebView(
  remoteMM: MicroModule.Runtime,
  options: DWebViewOptions,
): IDWebView {
  require(this is PureViewController)
  return IDWebView.Companion.create(remoteMM, options)
}