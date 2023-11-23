package org.dweb_browser.browser.desk

import android.annotation.SuppressLint
import android.view.Gravity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import org.dweb_browser.browser.R
import org.dweb_browser.dwebview.Render
import org.dweb_browser.dwebview.asAndroidWebView
import org.dweb_browser.helper.android.ActivityBlurHelper
import org.dweb_browser.helper.platform.IPureViewBox
import org.dweb_browser.helper.platform.PureViewBox
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.from
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.runBlockingCatching

@SuppressLint("ClickableViewAccessibility")
class TaskbarActivity : PureViewController() {

  private val blurHelper = ActivityBlurHelper(this)

  private var controller: TaskbarController? = null
  private fun bindController(sessionId: String?): DeskNMM.Companion.DeskControllers {
    /// 解除上一个 controller的activity绑定
    controller?.platformContext = null

    return DeskNMM.controllersMap[sessionId]?.also { controllers ->
      require(controllers.taskbarController is TaskbarController)
      controllers.taskbarController.platformContext = PureViewBox(this)
      controller = controllers.taskbarController
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  init {
    onCreate { params ->
      val (_, taskbarController) = bindController(params.getString("deskSessionId"))
      /// 禁止自适应布局
      WindowCompat.setDecorFitsSystemWindows(window, false)
      val densityValue =
        IPureViewBox.from(this@TaskbarActivity).getDisplayDensity();// resources.displayMetrics.density

      fun toPx(dp: Float) = (densityValue * dp).toInt()
      addContent {
        window.attributes = window.attributes.also { attributes ->
          taskbarController.state.composableHelper.apply {
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
          taskbarController.TaskbarView {
            taskbarDWebView.Render(onCreate = {
              setHorizontalScrollBarVisible(true)
              asAndroidWebView().setOnTouchListener { _, _ ->
                false
              }
            })
          }
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
    onDestroy {
      controller?.also { taskBarController ->
        runBlockingCatching {
          taskBarController.toggleFloatWindow(openTaskbar = false) // 销毁 TaskbarActivity 后需要将悬浮框重新显示加载
          taskBarController.platformContext = null
        }
      }
    }
    onTouch {
      if (it.x <= 0 || it.y <= 0 || it.x >= it.viewWidth || it.y >= it.viewHeight) {
        finish()
      }
    }
  }
}
