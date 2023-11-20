package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowController

@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectKeyboard() {
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectNavigationBar() {
}

@Composable
actual fun NativeBackHandler(
  enabled: Boolean,
  onBack: () -> Unit
) {
}