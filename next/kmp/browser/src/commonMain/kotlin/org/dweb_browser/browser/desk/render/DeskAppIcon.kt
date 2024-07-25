package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.model.DesktopAppModel
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.ImageResourcePurposes
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.render.AppIcon

@Composable
fun DeskAppIcon(
  icon: StrictImageResource?,
  microModule: NativeMicroModule.NativeRuntime,
  width: Dp,
  height: Dp,
  containerAlpha: Float? = null,
  containerColor: Color? = null,
  modifier: Modifier = Modifier,
) {
  val imageResult =
    icon?.let { PureImageLoader.SmartLoad(icon.src, width, height, microModule.blobFetchHook) }
  AppIcon(
    icon = imageResult,
    modifier = modifier.requiredSize(width, height),
    iconShape = deskSquircleShape(),
    iconMaskable = icon?.let { icon.purpose.contains(ImageResourcePurposes.Maskable) } ?: false,
    iconMonochrome = icon?.let { icon.purpose.contains(ImageResourcePurposes.Monochrome) } ?: false,
    containerAlpha = containerAlpha ?: deskIconAlpha,
    containerColor = containerColor
  )
}

internal val deskIconAlpha = when {
  canSupportModifierBlur() -> 0.9f
  else -> 1f
}

@Composable
internal fun DeskAppIcon(
  app: DesktopAppModel,
  microModule: NativeMicroModule.NativeRuntime,
  containerAlpha: Float? = null,
  containerColor: Color? = null,
  modifier: Modifier = Modifier,
) {
  val iconSize = desktopIconSize()
  DeskAppIcon(
    icon = app.icon,
    microModule = microModule,
    width = iconSize.width.dp,
    height = iconSize.height.dp,
    containerAlpha = containerAlpha,
    containerColor = containerColor,
    modifier = modifier.padding(8.dp)
  )
}