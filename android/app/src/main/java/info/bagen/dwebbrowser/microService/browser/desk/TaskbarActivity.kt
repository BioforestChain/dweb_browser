package info.bagen.dwebbrowser.microService.browser.desk

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.google.accompanist.web.WebView
import com.google.accompanist.web.rememberWebViewState
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.base.ActivityBlurHelper
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme

class TaskbarActivity : ComponentActivity() {
  private val blurHelper = ActivityBlurHelper(this)
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
      // val density = LocalDensity.current.density
      /// 改变窗口大小 和 一些属性
      window.setLayout((80 * density).toInt(), (200 * density).toInt());
      window.attributes = window.attributes.also { attributes ->
        /// 禁用模态窗口模式，使得点击可以向下穿透
        attributes.flags =
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        /// 将视图对其到右侧中央
        attributes.gravity = Gravity.CENTER_VERTICAL or Gravity.END;
      }

      DwebBrowserAppTheme {
        CompositionLocalProvider(
          LocalDesktopView provides taskBarController.createMainDwebView(),
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            /// 桌面视图
            val taskBarView = LocalDesktopView.current
            WebView(
              state = rememberWebViewState(url = taskBarController.getDesktopUrl().toString()),
              modifier = Modifier.fillMaxSize(),
            ) {
              taskBarView
            }
          }
        }
      }
    }

    /// 启用模糊
    blurHelper.config(
      backgroundBlurRadius = (10 * resources.displayMetrics.density).toInt(),
      windowBackgroundDrawable = getDrawable(R.drawable.taskbar_window_background)
    )
  }
}
