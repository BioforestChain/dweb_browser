package org.dweb_browser.sys.toast

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.getUIApplication
import org.dweb_browser.helper.withMainContext
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UITextAlignmentCenter
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionCurveEaseInOut
import platform.UIKit.bottomLayoutGuide
import platform.UIKit.topLayoutGuide

@OptIn(ExperimentalForeignApi::class)
actual suspend fun showToast(
  microModule: MicroModule.Runtime,
  text: String, durationType: ToastDurationType, positionType: ToastPositionType
) {
  withMainContext {
    val toastLabel = UILabel()
    toastLabel.setText(text)
    toastLabel.setTextColor(UIColor.whiteColor)
    toastLabel.setBackgroundColor(UIColor.blackColor.colorWithAlphaComponent(0.8))
    toastLabel.setTextAlignment(UITextAlignmentCenter)

    toastLabel.layer.setCornerRadius(10.0)
    toastLabel.layer.setMasksToBounds(true)
    val controller = microModule.getUIApplication().keyWindow?.rootViewController

    if (controller != null) {
      val width = controller.view.frame.useContents {
        size.width
      } - 40
      val height = toastLabel.sizeThatFits(CGSizeMake(width, CGFloat.MAX_VALUE)).useContents {
        height
      } + 20

      val x = (controller.view.frame.useContents { size.width } - width) / 2
      val y = when (positionType) {
        ToastPositionType.TOP -> controller.topLayoutGuide.length + 20
        ToastPositionType.CENTER -> (controller.view.frame.useContents { size.height } - height) / 2
        ToastPositionType.BOTTOM -> controller.view.frame.useContents { size.height } - controller.bottomLayoutGuide.length - height - 30
      }

      val duration = when (durationType) {
        ToastDurationType.SHORT -> 1.0
        ToastDurationType.LONG -> 3.0
      }

      toastLabel.setFrame(CGRectMake(x, y, width, height))
      controller.view.addSubview(toastLabel)
      UIView.animateWithDuration(
        duration = duration,
        delay = duration,
        UIViewAnimationOptionCurveEaseInOut,
        { toastLabel.setAlpha(0.0) }) { finished ->
        if (finished) {
          toastLabel.removeFromSuperview()
        }
      }
    }
  }
}