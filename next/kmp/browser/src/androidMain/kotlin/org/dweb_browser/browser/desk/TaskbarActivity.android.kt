package org.dweb_browser.browser.desk

import android.annotation.SuppressLint
import android.view.Gravity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import org.dweb_browser.browser.R
import org.dweb_browser.helper.android.ActivityBlurHelper
import org.dweb_browser.helper.compose.NativeBackHandler
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme

@SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables")
class TaskbarActivity : PureViewController() {

  private val blurHelper = ActivityBlurHelper(this)

  init {
    onCreate { params ->
      val sessionId = params.getString("deskSessionId")
      val taskbarController = DeskNMM.controllersMap[sessionId]?.run { getTaskbarController() }
        ?: throw Exception("no found controller by sessionId: $sessionId")

      onDestroy {
        @Suppress("DeferredResultUnused")
        taskbarController.toggleFloatWindow(openTaskbar = false) // 销毁 TaskbarActivity 后需要将悬浮框重新显示加载
      }
      /// 禁止自适应布局
      WindowCompat.setDecorFitsSystemWindows(window, false)

      addContent {
        val density = LocalDensity.current.density
        LaunchedEffect(density) {
          /// 启用模糊
          blurHelper.config(
            backgroundBlurRadius = (10 * density).toInt(),
            windowBackgroundDrawable = getDrawable(R.drawable.taskbar_window_background),
            dimAmountNoBlur = 0.3f,
            dimAmountWithBlur = 0.1f,
          )
        }

        fun toPx(dp: Float) = (density * dp).toInt()
        window.attributes = window.attributes.also { attributes ->
          taskbarController.state.apply {
            val layoutWidth by layoutWidthFlow.collectAsState()
            val layoutHeight by layoutHeightFlow.collectAsState()
            val layoutX by layoutXFlow.collectAsState()
            val layoutY by layoutYFlow.collectAsState()
            val layoutLeftPadding by layoutLeftPaddingFlow.collectAsState()
            val layoutTopPadding by layoutTopPaddingFlow.collectAsState()
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
          NativeBackHandler { finish() }
          /// 任务栏视图
          remember { taskbarController.getAndroidTaskbarView() }.InnerRender()
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
