package org.dweb_browser.browser.web

import org.dweb_browser.browser.web.model.BrowserViewModel

class BrowserViewModelIosHelper(private val browserViewModel: BrowserViewModel) {
  val download = BrowserViewModelDownloadImplementor(browserViewModel)

  fun destory() {
    download.destory()
  }
}

abstract class BrowserViewModelIosImplementor(internal val browserViewModel: BrowserViewModel) {
  abstract fun destory()
}
