package info.bagen.dwebbrowser.microService.browser.desk

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import info.bagen.dwebbrowser.R
import org.dweb_browser.core.module.BaseThemeActivity
import org.dweb_browser.helper.android.ActivityBlurHelper
import org.dweb_browser.helper.compose.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.runBlockingCatching

class TaskbarActivity : BaseThemeActivity() {

  private val blurHelper = ActivityBlurHelper(this)

  private var controller: TaskbarController? = null
  private fun bindController(sessionId: String?): DeskNMM.Companion.DeskControllers {
    /// 解除上一个 controller的activity绑定
    controller?.activity = null

    return DeskNMM.controllersMap[sessionId]?.also { controllers ->
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
      window.attributes = window.attributes.also { attributes ->
        taskbarController.taskbarView.state.composableHelper.apply {
          val layoutWidth by stateOf { layoutWidth }
          val layoutHeight by stateOf { layoutHeight }
          val layoutX by stateOf { layoutX }
          val layoutY by stateOf { layoutY }
          val layoutLeftPadding by stateOf { layoutLeftPadding }
          val layoutTopPadding by stateOf { layoutTopPadding }
          window.setLayout(
            toPx(layoutWidth),
            toPx(layoutHeight),
          )

          attributes.gravity = Gravity.TOP or Gravity.START
          attributes.x = toPx(layoutX - layoutLeftPadding)
          attributes.y = toPx(layoutY - layoutTopPadding)
        }
      }
      DwebBrowserAppTheme {
        BackHandler { finish() }
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

  override fun onDestroy() {
    super.onDestroy()
    controller?.also { taskBarController ->
      runBlockingCatching {
        taskBarController.taskbarView.toggleFloatWindow(openTaskbar = false) // 销毁 TaskbarActivity 后需要将悬浮框重新显示加载
        taskBarController.activity = null
      }
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
