package org.dweb_browser.browser.desk

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import org.dweb_browser.helper.compose.Language
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.render.LocalWindowsImeVisible

@OptIn(ExperimentalLayoutApi::class)
class DesktopActivity : PureViewController() {
  init {
    onCreate {
      /// 禁止自适应布局，执行后，可以将我们的内容嵌入到状态栏和导航栏，但是会发现我们的界面呗状态栏和导航栏给覆盖了，这时候就需要systemUiController来改颜色
      WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    addContent {
      BackHandler {
        moveTaskToBack(true) // 将界面移动到后台，避免重新点击又跑SplashActivity
      }
    }

    DesktopViewControllerCore(this)

    addContent {
      Language.InitLocalLanguage() // 初始化语言
      val imeVisible = LocalWindowsImeVisible.current
      val density = LocalDensity.current
      val ime =
        androidx.compose.foundation.layout.WindowInsets.imeAnimationTarget // 直接使用ime，数据不稳定，会变化，改为imeAnimationTarget就是固定值
      LaunchedEffect(ime) {
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
}