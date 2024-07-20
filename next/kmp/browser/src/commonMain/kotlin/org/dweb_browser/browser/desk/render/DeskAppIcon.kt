package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.core.module.NativeMicroModule

@Composable
internal fun DeskAppIcon(
  app: DesktopAppModel,
  microModule: NativeMicroModule.NativeRuntime,
  containerAlpha: Float? = null,
  modifier: Modifier = Modifier
) {
  val iconSize = desktopIconSize()
  DeskCacheIcon(
    app.icon,
    microModule,
    iconSize.width.dp,
    iconSize.height.dp,
    containerAlpha = containerAlpha,
    modifier.padding(8.dp)
  )
}