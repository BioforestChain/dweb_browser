package info.bagen.dwebbrowser.microService.browser.desk

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.base.BaseThemeActivity
import kotlinx.coroutines.launch
import org.dweb_browser.helper.android.ActivityBlurHelper
import org.dweb_browser.helper.android.theme.DwebBrowserAppTheme


class TaskbarActivity : BaseThemeActivity() {

  private val blurHelper = ActivityBlurHelper(this)

  private var controller: TaskbarController? = null
  private fun bindController(sessionId: String?): DesktopNMM.Companion.DeskControllers {
    /// 解除上一个 controller的activity绑定
    controller?.activity = null

    return DesktopNMM.controllers[sessionId]?.also { controllers ->
      controllers.taskbarController.activity = this
      controller = controllers.taskbarController
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  @SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val (_, taskbarController) = bindController(intent.getStringExtra("deskSessionId"))
    /// 禁止自适应布局
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val densityValue = resources.displayMetrics.density

    fun toPx(dp: Float) = (densityValue * dp).toInt()

    setContent {
      /// 关联到

      window.attributes = window.attributes.also { attributes ->
        taskbarController.taskbarView.apply {
          window.setLayout(
            toPx(layoutWidth.value),
            toPx(layoutHeight.value),
          )

          attributes.gravity = Gravity.TOP or Gravity.START
          attributes.x = toPx(layoutX.value - layoutLeftPadding.value)
          attributes.y = toPx(layoutY.value - layoutTopPadding.value)
        }
      }
      DwebBrowserAppTheme {
        BackHandler {
          finish()
        }
        /// 任务栏视图
        AndroidView(factory = {
          taskbarController.taskbarView.taskbarDWebView.also { webView ->
            webView.parent?.let { parent ->
              (parent as ViewGroup).removeView(webView)
            }
            webView.setOnTouchListener { _, _ ->
              false
            }
            webView.isHorizontalScrollBarEnabled = true
          }
        })
      }
    }

    /// 启用模糊
    blurHelper.config(
      backgroundBlurRadius = (10 * densityValue).toInt(),
      windowBackgroundDrawable = getDrawable(R.drawable.taskbar_window_background),
      dimAmountNoBlur = 0.3f,
      dimAmountWithBlur = 0.1f,
//      blurBehindRadius = (5 * density).toInt(),
    )
  }

  // Activity是否获得焦点
  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    this.lifecycleScope.launch {
      controller?.let { taskbarController ->
        taskbarController.getFocusApp()?.let { focusApp ->
          taskbarController.stateSignal.emit(
            TaskbarController.TaskBarState(hasFocus, focusApp)
          )
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    controller?.also { taskBarController ->
      taskBarController.taskbarView.closeFloatWindow() // 销毁 TaskbarActivity 后需要将悬浮框重新显示加载
      taskBarController.activity = null
    }

  }

  @SuppressLint("RestrictedApi")
  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    with(ev) {
      if (x <= 0 || y <= 0 || x >= window.decorView.width || y >= window.decorView.height) {
        finish()
      }
    }
    return super.dispatchTouchEvent(ev)
  }
}
