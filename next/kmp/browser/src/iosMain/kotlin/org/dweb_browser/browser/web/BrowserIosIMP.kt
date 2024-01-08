package org.dweb_browser.browser.web

import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.helper.OffListener
import platform.UIKit.UILabel
import platform.UIKit.UIView

interface IosInterface {
  fun getBrowserView(): UIView
  fun doSearch(key: String)
  fun colorSchemeChanged(color: Int)
  fun gobackIfCanDo(): Boolean
  fun browserActive(on: Boolean)
  fun browserClear()
//  fun isDarkColorScheme(isDark: Boolean)
}

class BrowserIosIMP() {

  private var imp: IosInterface? = null

  private var onWinVisibleListener: OffListener<Boolean>? = null
  private var onWinCloseListener: OffListener<Unit>? = null
  var browserViewModel: BrowserViewModel? = null
    set(value) {
      cancelViewModeObservers()
      value?.let {
        onWinVisibleListener = it.browserOnVisible { isVisiable ->
          browserVisiable(isVisiable)
        }
        onWinCloseListener = it.browserOnClose {
          browserClear()
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

  fun registerIosIMP(imp: IosInterface) {
    this.imp = imp
  }

  fun createIosMainView(): UIView {
    return imp?.getBrowserView() ?: UILabel().apply {
      this.text = "iOS Main View Load Fail"
    }
  }

  fun doSearch(key: String) {
    imp?.doSearch(key)
  }

  fun colorSchemeChanged(color: Int) {
    imp?.colorSchemeChanged(color)
  }

  fun gobackIfCanDo() = imp?.let { it.gobackIfCanDo() } ?: false

  fun browserVisiable(on: Boolean) {
    imp?.let {
      it.browserActive(on)
    }
  }

  fun browserClear() {
    imp?.let {
      it.browserClear()
    }
  }
}