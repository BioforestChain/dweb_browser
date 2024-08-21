package org.dweb_browser.sys.window.floatBar

import androidx.compose.ui.geometry.Offset
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.UIKit.UIGestureRecognizerStateBegan
import platform.UIKit.UIGestureRecognizerStateCancelled
import platform.UIKit.UIGestureRecognizerStateChanged
import platform.UIKit.UIGestureRecognizerStateEnded
import platform.UIKit.UIPanGestureRecognizer
import platform.UIKit.UIView
import platform.darwin.NSObject

class UIDragGesture(
  private val view: UIView,
  var draggableDelegate: DraggableDelegate,
  var density: Float,
) : NSObject() {
  fun setParams(draggableDelegate: DraggableDelegate, density: Float) {
    this.draggableDelegate = draggableDelegate
    this.density = density
  }

  @OptIn(ExperimentalForeignApi::class)
  fun getPoint(gesture: UIPanGestureRecognizer): Offset {
    val point = gesture.translationInView(view = view)
    val offset = point.useContents {
      Offset(x.toFloat(), y.toFloat())
    }
    return offset
  }

  private var prePoint = Offset.Zero

  @OptIn(BetaInteropApi::class)
  @ObjCAction
  fun dragView(gesture: UIPanGestureRecognizer) {

    when (gesture.state) {
      UIGestureRecognizerStateBegan -> {
        draggableDelegate.onDragStart(getPoint(gesture).also { prePoint = it })
      }

      UIGestureRecognizerStateChanged -> draggableDelegate.onDrag(getPoint(gesture).let {
        val diff = it - prePoint
        prePoint = it
        diff
      })

      UIGestureRecognizerStateEnded, UIGestureRecognizerStateCancelled -> draggableDelegate.onDragEnd()
    }
  }
}