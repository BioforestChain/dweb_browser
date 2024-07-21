package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.ImageResourcePurposes
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.blobFetchHook
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

@Composable
fun DeskCacheIcon(
  icon: StrictImageResource?,
  microModule: NativeMicroModule.NativeRuntime,
  width: Dp,
  height: Dp,
  containerAlpha: Float? = null,
  modifier: Modifier = Modifier,
) {
  val imageResult =
    icon?.let { PureImageLoader.SmartLoad(icon.src, width, height, microModule.blobFetchHook) }
  AppIcon(
    icon = imageResult,
    modifier = modifier.requiredSize(width, height),
    iconShape = SquircleShape(30, CornerSmoothing.Small),
    iconMaskable = icon?.let { icon.purpose.contains(ImageResourcePurposes.Maskable) } ?: false,
    iconMonochrome = icon?.let { icon.purpose.contains(ImageResourcePurposes.Monochrome) } ?: false,
    containerAlpha = containerAlpha ?: deskIconAlpha,
  )
}

internal const val deskIconAlpha = 0.8f