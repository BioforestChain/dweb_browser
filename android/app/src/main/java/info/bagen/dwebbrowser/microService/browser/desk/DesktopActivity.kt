package info.bagen.dwebbrowser.microService.browser.desk

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.google.accompanist.web.WebView
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.microService.browser.desk.view.Render
import info.bagen.dwebbrowser.microService.core.WindowBounds
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme

@SuppressLint("ModifierFactoryExtensionFunction")
fun WindowBounds.toModifier(
  modifier: Modifier = Modifier,
) = modifier
  .offset(left.dp, top.dp)
  .size(width.dp, height.dp)

class DesktopActivity : BaseActivity() {
  private var controller: DeskController? = null
  private fun bindController(sessionId: String?): DeskController {
    /// 解除上一个 controller的activity绑定
    controller?.activity = null

    return DesktopNMM.deskControllers[sessionId]?.also { desktopController ->
      desktopController.activity = this
      controller = desktopController
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val deskController = bindController(intent.getStringExtra("deskSessionId"))
    // TaskbarModel.init(intent.getStringExtra("taskBarSessionId") ?: "") // 初始化任务浮窗
    /*PermissionUtil.checkSuspendedWindowPermission(this) {
      startTaskbarService()
    }*/

    /**
     * 窗口管理器
     */
    val desktopWindowsManager = deskController.desktopWindowsManager

    setContent {
      DwebBrowserAppTheme {
        deskController.effect(activity = this@DesktopActivity)
        BackHandler {
          TaskbarModel.closeFloatWindow()
          this@DesktopActivity.moveTaskToBack(true) // 将界面移动到后台，避免重新点击又跑SplashActivity
        }

        CompositionLocalProvider(
          LocalInstallList provides deskController.getInstallApps(),
          LocalOpenList provides deskController.getOpenApps(),
          LocalDesktopView provides deskController.createMainDwebView(
            "desktop", deskController.getDesktopUrl().toString()
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
            /// 悬浮框
            TaskbarModel.FloatWindow()
          }
        }
      }
    }
  }

  private var isPause = false

  override fun onResume() {
    TaskbarModel.openFloatWindow()
    isPause = false
    super.onResume()
  }

  override fun onPause() {
    isPause = true
    super.onPause()
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    if (!hasFocus && !isPause) { // 验证发现，如果通过系统上滑退出会先执行失焦，然后才走到onPause，其他情况都会先执行onPause
      TaskbarModel.closeFloatWindow()
    }
    super.onWindowFocusChanged(hasFocus)
  }

  /*override fun onActivityResult(requestCode: kotlin.Int, resultCode: kotlin.Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 999) {
      if (Settings.canDrawOverlays(this)) {
        startTaskbarService()
      } else {
        PermissionUtil.checkSuspendedWindowPermission(this@DesktopActivity) {
          startTaskbarService()
        }
      }
    }
  }

  private fun startTaskbarService() {
    startService(Intent(this@DesktopActivity, TaskbarService::class.java))
  }*/
}
