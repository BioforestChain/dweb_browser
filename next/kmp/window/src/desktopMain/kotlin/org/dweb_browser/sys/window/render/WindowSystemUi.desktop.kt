package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager

@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectKeyboard() {
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectNavigationBar() {
}

@Composable
actual fun NativeBackHandler(enabled: Boolean, onBack: () -> Unit) {
}

/**
 * 截屏、录像控制
 */
@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectSafeModel() {
  WARNING("Java Swing No Support Safe Modal")
}