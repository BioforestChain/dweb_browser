package org.dweb_browser.window.render

import androidx.compose.runtime.Composable
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowsManager


@Composable
expect fun <T : WindowController> WindowsManager<T>.EffectKeyboard(): Unit

@Composable
expect fun <T : WindowController> WindowsManager<T>.EffectNavigationBar(): Unit