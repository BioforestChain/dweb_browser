package org.dweb_browser.dwebview.engine

import com.teamdev.jxbrowser.browser.Browser
import com.teamdev.jxbrowser.view.swing.BrowserView
import org.dweb_browser.dwebview.DWebViewOptions

abstract class DWebViewEngine(val options: DWebViewOptions) {
  abstract val browser: Browser
  val wrapperView by lazy { BrowserView.newInstance(browser) }
}