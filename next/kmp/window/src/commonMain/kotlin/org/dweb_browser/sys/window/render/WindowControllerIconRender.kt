package org.dweb_browser.sys.window.render

import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.helper.watchedState

/**
 * 图标渲染
 */
@Composable
fun WindowController.IconRender(
  modifier: Modifier = Modifier,
  primaryColor: Color = LocalContentColor.current,
  primaryContainerColor: Color? = null,
) {
  val iconUrl by watchedState { iconUrl }
  val iconMaskable by watchedState { iconMaskable }
  val iconMonochrome by watchedState { iconMonochrome }
  val microModule by state.constants.microModule
  AppLogo.fromUrl(
    iconUrl, fetchHook = microModule?.blobFetchHook, base = AppLogo(
      color = primaryColor,
      monochrome = iconMonochrome,
      maskable = iconMaskable,
    )
  ).toIcon(AppIconContainer(color = primaryContainerColor, alpha = 0.2f)).Render(modifier)
}