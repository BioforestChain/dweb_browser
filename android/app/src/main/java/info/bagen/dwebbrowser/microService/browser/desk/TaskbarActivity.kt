package info.bagen.dwebbrowser.microService.browser.desk

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.ui.viewinterop.AndroidView
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.base.ActivityBlurHelper
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme

class TaskbarActivity : BaseActivity() {

  private val blurHelper = ActivityBlurHelper(this)

  private val taskbarViewModel by taskAppViewModels<TaskbarViewModel>()

  private var controller: TaskBarController? = null
  private fun bindController(sessionId: String?): TaskBarController {
    /// 解除上一个 controller的activity绑定
    controller?.activity = null

    return DesktopNMM.taskBarControllers[sessionId]?.also { taskBarController ->
      taskBarController.activity = this
      controller = taskBarController
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  @SuppressLint("UseCompatLoadingForDrawables")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val taskBarController = bindController(intent.getStringExtra("taskBarSessionId"))
    val density = resources.displayMetrics.density

    setContent {
      /// 改变窗口大小 和 一些属性
      /// window.setLayout((80 * density).toInt(), (200 * density).toInt())
      val layoutWidth = maxOf(taskbarViewModel.width, (80 * density).toInt())
      val layoutHeight = maxOf(taskbarViewModel.height, (200 * density).toInt())
      window.setLayout(layoutWidth, layoutHeight)
      window.attributes = window.attributes.also { attributes ->
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
          taskbarViewModel.taskbarDWebView.also { webView ->
            webView.parent?.let { parent ->
              (parent as ViewGroup).removeView(webView)
            }
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

  override fun onDestroy() {
    super.onDestroy()
    taskbarViewModel.floatViewState.value = true // 销毁 TaskbarActivity 后需要将悬浮框重新显示加载
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
