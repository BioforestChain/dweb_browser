package org.dweb_browser.browser.common


import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.WebColorScheme
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.WindowColorScheme

expect suspend fun WindowController.createDwebView(
  remoteMM: MicroModule.Runtime,
  url: String,
): IDWebView

fun WindowColorScheme.toWebColorScheme() = when (this) {
  WindowColorScheme.Dark -> WebColorScheme.Dark
  WindowColorScheme.Light -> WebColorScheme.Light
  WindowColorScheme.Normal -> WebColorScheme.Normal
}