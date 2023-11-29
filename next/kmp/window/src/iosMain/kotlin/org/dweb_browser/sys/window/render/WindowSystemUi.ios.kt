package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import org.dweb_browser.helper.WARNING
import org.dweb_browser.sys.window.core.WindowsManager
import org.dweb_browser.sys.window.core.WindowController

@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectKeyboard() {
  WARNING("Not yet implemented EffectKeyboard")
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectNavigationBar() {
  WARNING("Not yet implemented EffectNavigationBar")
}

@Composable
actual fun NativeBackHandler(
  enabled: Boolean,
  onBack: () -> Unit
) {
  WARNING("Not yet implemented NativeBackHandler")
}

@Composable
actual fun <T : WindowController> WindowsManager<T>.EffectSafeModel() {
  WARNING("Not yet implemented EffectSafeModel")
}