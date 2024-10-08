package org.dweb_browser.browser.desk

import android.transition.Fade
import android.view.ViewTreeObserver
import android.view.Window
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import org.dweb_browser.helper.compose.NativeBackHandler
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.helper.LocalWindowsImeVisible

@OptIn(ExperimentalLayoutApi::class)
class DesktopActivity : PureViewController() {

  init {
    onCreate {
      with(window) {
        requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        allowEnterTransitionOverlap = true
        allowReturnTransitionOverlap = true

        enterTransition = Fade()
      }
      /// 禁止自适应布局，执行后，可以将我们的内容嵌入到状态栏和导航栏，但是会发现我们的界面呗状态栏和导航栏给覆盖了，这时候就需要systemUiController来改颜色
      WindowCompat.setDecorFitsSystemWindows(window, false)
      // lifecycleScope.launch { uploadDeviceInfo(this@DesktopActivity) } // 提交系统信息
    }

    addContent {
      NativeBackHandler {
        moveTaskToBack(true) // 将界面移动到后台，避免重新点击又跑SplashActivity
      }
    }

    DeskViewController(this)

    addContent {
      /// 对 imeVisible 的绑定支持
      val imeVisibleState = LocalWindowsImeVisible.current
      val density = LocalDensity.current
      val imeInsets = WindowInsets.imeAnimationTarget // 直接使用ime，数据不稳定，会变化，改为imeAnimationTarget就是固定值
      DisposableEffect(imeInsets, density) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
          imeVisibleState.value = imeInsets.getBottom(density) != 0
        }
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(listener)
        onDispose {
          window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
      }
    }
  }
}
