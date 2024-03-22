package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager

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

@Composable
actual fun <T : WindowController> WindowsManager<T>.WindowsManagerEffect() {
  /// 键盘的互操作性
  EffectKeyboard()
  /// 窗口截屏安全限制
  EffectSafeModel()
}