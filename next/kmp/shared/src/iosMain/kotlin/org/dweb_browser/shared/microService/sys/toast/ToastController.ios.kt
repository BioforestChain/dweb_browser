package org.dweb_browser.shared.microService.sys.toast

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UILabel
import platform.UIKit.UITextAlignmentCenter
import platform.UIKit.UIView
import platform.UIKit.UIViewAnimationOptionCurveEaseOut
import platform.UIKit.UIViewController
import platform.UIKit.bottomLayoutGuide
import platform.UIKit.topLayoutGuide

actual object ToastController {

    actual fun show(
        text: String,
        durationType: DurationType,
        positionType: PositionType
    ) {
        val toastLabel = generateToastLabel(text)
        val controller = UIApplication.sharedApplication.keyWindow?.rootViewController

        var duration = when(durationType) {
            DurationType.SHORT -> 2.0
            DurationType.LONG ->3.5
        }

        if (controller != null) {
            generateFrame(controller!!,toastLabel,positionType)
            controller!!.view.addSubview(toastLabel)

            UIView.animateWithDuration(
                duration,
                duration,
                UIViewAnimationOptionCurveEaseOut,
                animations = { toastLabel.alpha = 0.0 },
                completion = { toastLabel.removeFromSuperview() })
        }
    }

    private fun generateToastLabel(text: String): UILabel {
        val toastLabel = UILabel()
        toastLabel.text = text
        toastLabel.backgroundColor = UIColor.blackColor().colorWithAlphaComponent(0.8)
        toastLabel.textColor = UIColor.whiteColor
        toastLabel.textAlignment = UITextAlignmentCenter
        toastLabel.layer.cornerRadius = 10.0
        toastLabel.layer.masksToBounds = true
        toastLabel.font = UIFont.systemFontOfSize(16.0)
        return toastLabel
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun generateFrame(controller: UIViewController,
                              toastLabel: UILabel,
                              positionType: PositionType) {

        var totalSize: CGSize
        var x: CGFloat = 0.0
        var y: CGFloat = 0.0
        var tmpWidth: CGFloat = 0.0
        var tmpHeight: CGFloat = 0.0

        controller.view.frame().useContents {
            totalSize = this.size
            toastLabel.sizeThatFits(CGSizeMake(totalSize.width - 40, totalSize.height))
                .useContents {
                    tmpWidth = this.width
                    tmpHeight = this.height
                }
            x = (totalSize.width - tmpWidth - 40) * 0.5
            y = when(positionType) {
                PositionType.TOP -> controller.topLayoutGuide.length + 20
                PositionType.CENTER -> (totalSize.height - tmpHeight - 20) * 0.5
                PositionType.BOTTOM -> totalSize.height - controller.bottomLayoutGuide.length - tmpHeight - 20
            }
        }

        toastLabel.setFrame(
            platform.CoreGraphics.CGRectMake(
                x,
                y,
                tmpWidth + 40,
                tmpHeight + 20
            )
        )
    }
}