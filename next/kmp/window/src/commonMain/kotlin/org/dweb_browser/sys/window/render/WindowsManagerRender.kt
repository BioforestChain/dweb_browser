package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowsManager

@Composable
expect fun <T : WindowController> WindowsManager<T>.SceneRender(modifier: Modifier = Modifier)
