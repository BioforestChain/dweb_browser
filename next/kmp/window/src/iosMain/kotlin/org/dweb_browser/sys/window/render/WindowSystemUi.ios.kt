package org.dweb_browser.window.render

import androidx.compose.runtime.Composable
import org.dweb_browser.window.core.WindowsManager
import org.dweb_browser.window.core.WindowController

@Composable
actual fun <T:WindowController>WindowsManager<T>.EffectKeyboard() {
}

@Composable
actual fun <T:WindowController>WindowsManager<T>.EffectNavigationBar() {
}

@Composable
actual fun WindowController.BackHandler(
  enabled: Boolean,
  onBack: () -> Unit
) {
}