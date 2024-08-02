package org.dweb_browser.browser.jmm.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.render.DeskAppIcon
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.sys.window.core.constant.LocalWindowMM

@Composable
fun JmmAppInstallManifest.IconRender(size: Dp = 36.dp) {
  DeskAppIcon(
    icons = icons,
    microModule = LocalWindowMM.current,
    width = size,
    height = size,
    containerColor = Color.White,
  )
}