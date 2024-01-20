package org.dweb_browser.sys.mediacapture

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.helper.toRect
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIColor
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
class VideoButton(frame: CValue<CGRect>) : UIView(frame = frame) {

    private var peripheryView: UIView? = null
    private var internalView: UIView? = null
    private var distance: CGFloat = 8.0

    init {
        createPeripheryView()
        createInternalView()
    }

    @OptIn(ExperimentalForeignApi::class)
    fun updateInternalView(isRecording: Boolean) {

        val rect = this.frame.toRect()

        UIView.animateWithDuration(0.25) {
            if (isRecording) {
                this.distance += 10.0
                this.internalView?.layer?.cornerRadius = 4.0
            } else {
                this.distance -= 10.0
                this.internalView?.layer?.cornerRadius = (rect.width - distance * 2) * 0.5
            }
            this.internalView!!.setFrame(CGRectMake(
                this.distance,
                this.distance,
                (rect.width - this.distance * 2),
                (rect.height - this.distance * 2)
            ))
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createPeripheryView() {
        val rect = this.frame.toRect()
        val peripheryView = UIView(CGRectMake(
            0.0,
            0.0,
            rect.width.toDouble(),
            rect.height.toDouble()))

        peripheryView.backgroundColor = UIColor.whiteColor
        peripheryView.layer.borderWidth = 4.0
        peripheryView.layer.borderColor = UIColor(
            red = 113 / 255.0,
            green = 92 / 255.0,
            blue = 112 / 255.0,
            alpha = 1.0
        ).CGColor

        peripheryView.layer.cornerRadius = rect.width * 0.5
        peripheryView.layer.masksToBounds = true
        this.peripheryView = peripheryView
        this.addSubview(peripheryView)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun createInternalView() {
        val rect = this.frame.toRect()
        val internalView = UIView(CGRectMake(
            distance,
            distance,
            (rect.width - distance * 2),
            (rect.height - distance * 2)
        ))
        internalView.backgroundColor = UIColor.redColor
        internalView.layer.cornerRadius = (rect.width - distance * 2) * 0.5
        internalView.layer.masksToBounds = true
        this.internalView = internalView
        this.addSubview(internalView)
    }
}