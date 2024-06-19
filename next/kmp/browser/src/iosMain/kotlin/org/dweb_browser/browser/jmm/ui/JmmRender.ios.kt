package org.dweb_browser.browser.jmm.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.browser.jmm.JmmRenderController
import org.dweb_browser.sys.window.core.WindowContentRenderScope

@Composable
actual fun JmmRenderController.Render(
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope,
) {
  CommonRender(modifier, windowRenderScope)
}