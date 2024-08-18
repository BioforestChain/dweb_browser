package org.dweb_browser.dwebview.engine

import android.content.pm.ActivityInfo
import android.view.View
import android.view.WindowManager.LayoutParams
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.dweb_browser.helper.android.BaseActivity

class DWebCustomView(
  private val activity: BaseActivity?
) : WebChromeClient() {
  private var customView: View? = null
  private var customViewCallback: CustomViewCallback? = null
  private var originalOrientation: Int =
    activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
  private var beforeSystemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT

  override fun onHideCustomView() {
    activity?.also { fullscreenVideoActivity ->
      if (customView == null) {
        return
      }
      val window = fullscreenVideoActivity.window
      fullscreenVideoActivity.requestedOrientation = originalOrientation
      val decor = window.decorView as FrameLayout

      decor.removeView(customView)
      customView = null
      customViewCallback?.onCustomViewHidden()
      customViewCallback = null

      with(WindowInsetsControllerCompat(window, window.decorView)) {
        systemBarsBehavior = beforeSystemBarsBehavior
        show(WindowInsetsCompat.Type.systemBars())
      }
    }
  }

  override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
    activity?.also { fullscreenVideoActivity ->
      if (customView != null) {
        onHideCustomView()
        return
      }

      customView = view
      customViewCallback = callback

      if (customView is FrameLayout) {
        val window = fullscreenVideoActivity.window
        fullscreenVideoActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val decor = window.decorView as FrameLayout
        decor.addView(
          customView as FrameLayout,
          LayoutParams.MATCH_PARENT,
          LayoutParams.MATCH_PARENT
        )

        with(WindowInsetsControllerCompat(window, window.decorView)) {
          beforeSystemBarsBehavior = systemBarsBehavior
          systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
          hide(WindowInsetsCompat.Type.systemBars())
        }
      }
    }
  }
}