package info.bagen.dwebbrowser.microService.browser.desk

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.web.WebView
import info.bagen.dwebbrowser.base.BaseThemeActivity
import org.dweb_browser.helper.compose.theme.DwebBrowserAppTheme
import org.dweb_browser.window.core.WindowBounds
import org.dweb_browser.window.render.Render

@SuppressLint("ModifierFactoryExtensionFunction")
fun WindowBounds.toModifier(
  modifier: Modifier = Modifier,
) = modifier
  .offset(left.dp, top.dp)
  .size(width.dp, height.dp)

class DesktopActivity : BaseThemeActivity() {
  private var desktopController: DesktopController? = null
  private fun bindController(sessionId: String?): DesktopNMM.Companion.DeskControllers {
    /// 解除上一个 controller的activity绑定
    desktopController?.activity = null

    return DesktopNMM.controllers[sessionId]?.also { controllers ->
      controllers.desktopController.activity = this
      this.desktopController = controllers.desktopController
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val (desktopController, taskbarController) = bindController(intent.getStringExtra("deskSessionId"))
    /// 禁止自适应布局
    WindowCompat.setDecorFitsSystemWindows(window, false)
    /**
     * 窗口管理器
     */
    val desktopWindowsManager = desktopController.desktopWindowsManager
    setContent {
      BackHandler {
        this@DesktopActivity.moveTaskToBack(true) // 将界面移动到后台，避免重新点击又跑SplashActivity
      }

      DwebBrowserAppTheme {
        CompositionLocalProvider(
          LocalDesktopView provides desktopController.createMainDwebView(
            "desktop", desktopController.getDesktopUrl().toString()
          ),
        ) {
          Box {
            /// 桌面视图
            val desktopView = LocalDesktopView.current
            WebView(
              state = desktopView.state,
              navigator = desktopView.navigator,
              modifier = Modifier.fillMaxSize(),
            ) {
              desktopView.webView
            }
            /// 窗口视图
            desktopWindowsManager.Render()
          }
          /// 悬浮框
          Box(modifier = Modifier.fillMaxSize(), Alignment.TopStart) {
            taskbarController.taskbarView.FloatWindow()
          }
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    desktopController?.activity = null
  }
}
