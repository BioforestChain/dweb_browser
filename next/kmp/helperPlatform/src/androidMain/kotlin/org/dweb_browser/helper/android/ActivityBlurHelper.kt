package org.dweb_browser.helper.android

import android.app.Activity
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import java.util.function.Consumer

/**
 * 一个将Activity进行背景模糊的工具
 * 参阅： https://source.android.com/docs/core/display/window-blurs?hl=zh-cn
 */
class ActivityBlurHelper(val activity: Activity) {
  /**
   * Activity 视图背景模糊
   */
  private var mBackgroundBlurRadius = 20

  /**
   * Activity 视图遮罩模糊
   */
  private var mBlurBehindRadius = 0

  // We set a different dim amount depending on whether window blur is enabled or disabled
  private var mDimAmountWithBlur = 0.4f
  private var mDimAmountNoBlur = 0.4f

  // We set a different alpha depending on whether window blur is enabled or disabled
  private var mWindowBackgroundAlphaWithBlur = 170
  private var mWindowBackgroundAlphaNoBlur = 255

  // Use a rectangular shape drawable for the window background. The outline of this drawable
  // dictates the shape and rounded corners for the window background blur area.
  private var mWindowBackgroundDrawable: Drawable? = null

  fun config(
    backgroundBlurRadius: Int? = null,
    blurBehindRadius: Int? = null,
    dimAmountWithBlur: Float? = null,
    dimAmountNoBlur: Float? = null,
    windowBackgroundAlphaWithBlur: Int? = null,
    windowBackgroundAlphaNoBlur: Int? = null,
    windowBackgroundDrawable: Drawable? = null,
  ) {
    backgroundBlurRadius?.also { this.mBackgroundBlurRadius = it };
    blurBehindRadius?.also { this.mBlurBehindRadius = it };
    dimAmountWithBlur?.also { this.mDimAmountWithBlur = it };
    dimAmountNoBlur?.also { this.mDimAmountNoBlur = it };
    windowBackgroundAlphaWithBlur?.also { this.mWindowBackgroundAlphaWithBlur = it };
    windowBackgroundAlphaNoBlur?.also { this.mWindowBackgroundAlphaNoBlur = it };
    windowBackgroundDrawable?.also {
      this.mWindowBackgroundDrawable = it
      activity.window.setBackgroundDrawable(mWindowBackgroundDrawable)
    }
    this.updateWindowForBlurs()
  }

  private var inited = false
  private fun setup(): Boolean {
    if (inited) {
      return false;
    }
    inited = true
    with(activity) {
      if (buildIsAtLeastS()) {
        // Enable blur behind. This can also be done in xml with R.attr#windowBlurBehindEnabled
        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)

        // Register a listener to adjust window UI whenever window blurs are enabled/disabled
        setupWindowBlurListener()
        // Enable window blurs
        updateWindowForBlurs(true)
      } else {
        // Window blurs are not available prior to Android S
        updateWindowForBlurs(false /* blursEnabled */)
      }
      window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    return true
  }

  /**
   * Set up a window blur listener.
   *
   * Window blurs might be disabled at runtime in response to user preferences or system states
   * (e.g. battery saving mode). WindowManager#addCrossWindowBlurEnabledListener allows to
   * listen for when that happens. In that callback we adjust the UI to account for the
   * added/missing window blurs.
   *
   * For the window background blur we adjust the window background drawable alpha:
   * - lower when window blurs are enabled to make the blur visible through the window
   * background drawable
   * - higher when window blurs are disabled to ensure that the window contents are readable
   *
   * For window blur behind we adjust the dim amount:
   * - higher when window blurs are disabled - the dim creates a depth of field effect,
   * bringing the user's attention to the dialog window
   * - lower when window blurs are enabled - no need for a high alpha, the blur behind is
   * enough to create a depth of field effect
   */
  @RequiresApi(api = Build.VERSION_CODES.S)
  private fun setupWindowBlurListener() {
    val windowBlurEnabledListener = Consumer<Boolean> { blursEnabled: Boolean ->
      updateWindowForBlurs(blursEnabled)
    }
    with(activity) {
      window.decorView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
          windowManager.addCrossWindowBlurEnabledListener(
            windowBlurEnabledListener
          )
        }

        override fun onViewDetachedFromWindow(v: View) {
          windowManager.removeCrossWindowBlurEnabledListener(
            windowBlurEnabledListener
          )
        }
      })
    }
  }

  private var _blursEnabled = false
  private fun updateWindowForBlurs(blursEnabled: Boolean = this._blursEnabled) {
    this._blursEnabled = blursEnabled
    with(activity) {
      mWindowBackgroundDrawable?.alpha =
        if (blursEnabled && mBackgroundBlurRadius > 0) mWindowBackgroundAlphaWithBlur else mWindowBackgroundAlphaNoBlur

      window.setDimAmount(if (blursEnabled && mBlurBehindRadius > 0) mDimAmountWithBlur else mDimAmountNoBlur)
      if (buildIsAtLeastS()) {
        // Set the window background blur and blur behind radii
        window.setBackgroundBlurRadius(mBackgroundBlurRadius)
        window.attributes.blurBehindRadius = mBlurBehindRadius
        window.attributes = window.attributes
      }
    }
  }

  private fun buildIsAtLeastS(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
  }
}