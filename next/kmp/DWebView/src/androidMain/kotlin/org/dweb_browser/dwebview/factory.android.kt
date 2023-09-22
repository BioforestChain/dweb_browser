package org.dweb_browser.dwebview

import org.dweb_browser.helper.platform.PlatformViewController
import org.dweb_browser.microservice.core.MicroModule

actual fun createDWebView(
  platformViewController: PlatformViewController,
  remoteMM: MicroModule
): IDWebView {
  return DWebView(platformViewController.androidContext, remoteMM, DWebView.Options())
}