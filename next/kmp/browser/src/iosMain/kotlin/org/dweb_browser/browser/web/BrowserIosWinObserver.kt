package org.dweb_browser.browser.web

import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.helper.OffListener

class BrowserIosWinObserver(val winVisibleChange: ((Boolean) -> Unit), val winClose: (() -> Unit)) {

  private var onWinVisibleListener: OffListener<Boolean>? = null
  private var onWinCloseListener: OffListener<Unit>? = null

  var browserViewModel: BrowserViewModel? = null
    set(value) {
      cancelViewModeObservers()
      value?.let {
        onWinVisibleListener = it.browserOnVisible { isVisiable ->
          winVisibleChange(isVisiable)
        }
        onWinCloseListener = it.browserOnClose {
          winClose()
          cancelViewModeObservers()
        }
      }
    }

  private fun cancelViewModeObservers() {
    onWinVisibleListener?.let {
      it()
    }
    onWinCloseListener?.let {
      it()
    }
  }
}