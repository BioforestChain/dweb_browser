package org.dweb_browser.dwebview.engine

import platform.UIKit.UIScrollView
import platform.UIKit.UIScrollViewDelegateProtocol
import platform.UIKit.UIView
import platform.darwin.NSObject

class DWebUIScrollViewDelegate(val engine: DWebViewEngine) : NSObject(),
  UIScrollViewDelegateProtocol {
  override fun scrollViewWillBeginZooming(scrollView: UIScrollView, withView: UIView?) {
    scrollView.pinchGestureRecognizer?.setEnabled(false)
  }
}