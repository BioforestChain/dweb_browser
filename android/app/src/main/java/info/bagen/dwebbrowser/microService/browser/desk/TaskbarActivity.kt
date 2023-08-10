package info.bagen.dwebbrowser.microService.browser.desk

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.coroutineScope
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.base.ActivityBlurHelper
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme
import kotlinx.coroutines.launch

class TaskbarActivity : BaseActivity() {

  private val blurHelper = ActivityBlurHelper(this)

  private var controller: TaskBarController = DesktopNMM.taskBarController

  @SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    controller.activity  = this
    val density = resources.displayMetrics.density

    setContent {
      window.attributes = window.attributes.also { attributes ->
        window.setLayout(controller.cacheResize.width,controller.cacheResize.height)
//        /// 禁用模态窗口模式，使得点击可以向下穿透
//        attributes.flags =
//          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        /// 将视图对其到右侧中央
        attributes.gravity = Gravity.CENTER_VERTICAL or Gravity.END;
      }
      DwebBrowserAppTheme {
        BackHandler {
          finish()
        }
        /// 任务栏视图
        AndroidView(factory = {
          TaskbarModel.taskbarDWebView.also { webView ->
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
      backgroundBlurRadius = (10 * density).toInt(),
      windowBackgroundDrawable = getDrawable(R.drawable.taskbar_window_background),
      dimAmountNoBlur = 0.3f,
      dimAmountWithBlur = 0.1f,
//      blurBehindRadius = (5 * density).toInt(),
    )
  }

  // Activity是否获得焦点
  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    this.lifecycle.coroutineScope.launch {
      controller.let { taskBarController ->
        taskBarController.stateSignal.emit(
          TaskBarController.TaskBarState(hasFocus, taskBarController.getFocusApp())
        )
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    TaskbarModel.openFloatWindow() // 销毁 TaskbarActivity 后需要将悬浮框重新显示加载
    controller.activity = null
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
