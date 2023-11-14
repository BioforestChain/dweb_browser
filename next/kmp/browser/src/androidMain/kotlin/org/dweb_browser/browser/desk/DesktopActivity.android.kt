package org.dweb_browser.browser.desk

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import org.dweb_browser.core.module.BaseThemeActivity
import org.dweb_browser.helper.compose.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.platform.PlatformViewController
import org.dweb_browser.sys.window.core.Rect
import org.dweb_browser.sys.window.render.LocalWindowsImeVisible

@SuppressLint("ModifierFactoryExtensionFunction")
fun Rect.toModifier(
  modifier: Modifier = Modifier,
) = modifier
  .offset(x.dp, y.dp)
  .size(width.dp, height.dp)

class DesktopActivity : BaseThemeActivity() {
  private var desktopController: DesktopController? = null
  private fun bindController(sessionId: String?): DeskNMM.Companion.DeskControllers {
    /// 解除上一个 controller的activity绑定
    desktopController?.activity = null

    return DeskNMM.controllersMap[sessionId]?.also { controllers ->
      require(controllers.desktopController is DesktopController)
      controllers.desktopController.activity = this
      this.desktopController = controllers.desktopController
      controllers.activityPo.resolve(PlatformViewController(this))
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  @OptIn(ExperimentalLayoutApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val (desktopController, taskbarController, microModule) = bindController(intent.getStringExtra("deskSessionId"))
    require(desktopController is DesktopController && taskbarController is TaskbarController)
    /// 禁止自适应布局，执行后，可以将我们的内容嵌入到状态栏和导航栏，但是会发现我们的界面呗状态栏和导航栏给覆盖了，这时候就需要systemUiController来改颜色
    WindowCompat.setDecorFitsSystemWindows(window, false)
    setContent {
      val imeVisible = LocalWindowsImeVisible.current
      val density = LocalDensity.current
      val ime =
        androidx.compose.foundation.layout.WindowInsets.imeAnimationTarget // 直接使用ime，数据不稳定，会变化，改为imeAnimationTarget就是固定值
      BackHandler {
        this@DesktopActivity.moveTaskToBack(true) // 将界面移动到后台，避免重新点击又跑SplashActivity
      }

      DwebBrowserAppTheme {
        desktopController.Render(taskbarController, microModule)
      }

      LaunchedEffect(Unit) {
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
          /*val rect = android.graphics.Rect()
          window.decorView.getWindowVisibleDisplayFrame(rect)
          val screenHeight = window.decorView.rootView.height
          val screenDifference = screenHeight - rect.bottom
          val visible =  screenDifference > screenHeight / 3
          imeVisible.value = visible*/
          imeVisible.value = ime.getBottom(density) != 0
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    desktopController?.activity = null
  }
}
