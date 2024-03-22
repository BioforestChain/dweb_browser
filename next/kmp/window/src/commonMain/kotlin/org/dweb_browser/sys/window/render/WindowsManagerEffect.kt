package org.dweb_browser.sys.window.render


import androidx.compose.runtime.Composable
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager


@Composable
expect fun <T : WindowController> WindowsManager<T>.WindowsManagerEffect()
