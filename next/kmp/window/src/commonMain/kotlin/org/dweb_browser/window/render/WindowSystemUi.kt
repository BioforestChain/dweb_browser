package org.dweb_browser.window.render

import androidx.compose.runtime.Composable
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowsManager


@Composable
expect fun <T : WindowController> WindowsManager<T>.EffectKeyboard()

@Composable
expect fun <T : WindowController> WindowsManager<T>.EffectNavigationBar()

@Composable
expect fun WindowController.BackHandler(enabled: Boolean, onBack: () -> Unit)
