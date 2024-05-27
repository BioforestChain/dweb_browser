package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.painter.Painter
import dweb_browser_kmp.window.generated.resources.Res
import dweb_browser_kmp.window.generated.resources.dweb_browser
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.platform.ComposeWindowParams
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager
import org.jetbrains.compose.resources.painterResource

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

private val appIcon = mutableStateOf<Painter?>(null)

@Composable
actual fun <T : WindowController> WindowsManager<T>.WindowsManagerEffect() {
  if (appIcon.value == null) {
    appIcon.value = painterResource(Res.drawable.dweb_browser)
    ComposeWindowParams.defaultIcon = appIcon.value
  }
  /// 键盘的互操作性
  EffectKeyboard()
  /// 窗口截屏安全限制
  EffectSafeModel()
}