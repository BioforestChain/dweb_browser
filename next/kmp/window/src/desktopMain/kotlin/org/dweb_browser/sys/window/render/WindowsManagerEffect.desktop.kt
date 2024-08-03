package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import dweb_browser_kmp.window.generated.resources.Res
import dweb_browser_kmp.window.generated.resources.dweb_browser
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.compose.asState
import org.dweb_browser.helper.platform.ComposeWindowParams
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.jetbrains.compose.resources.painterResource
import java.awt.Image

@Composable
private fun <T : WindowController> WindowsManager<T>.EffectKeyboard() {
  WARNING("Java Swing No Support Virtual EffectKeyboard")
}

/**
 * 截屏、录像控制
 */
@Composable
private fun <T : WindowController> WindowsManager<T>.EffectSafeModel() {
  WARNING("Java Swing No Support Safe Modal")
}

class RootAppIcon(
  val painter: Painter,
  val awtImage: Image,
)

val appIcon by lazy {
  CompletableDeferred<RootAppIcon>().also { deferred ->
    val key = "app-icon-render"
    PureViewController.contents += key to {
      val painter = painterResource(Res.drawable.dweb_browser)
      val awtImage = painter.toAwtImage(
        LocalDensity.current, LocalLayoutDirection.current,
        Size(64f, 64f)
      )
      deferred.complete(RootAppIcon(painter, awtImage))
      PureViewController.contents -= key
    }
  }
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.WindowsManagerEffect() {
  ComposeWindowParams.defaultIcon = appIcon.asState().value?.painter
  /// 键盘的互操作性
  EffectKeyboard()
  /// 窗口截屏安全限制
  EffectSafeModel()
}