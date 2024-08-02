package org.dweb_browser.sys.window.render


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import org.dweb_browser.helper.ImageResourcePurposes
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.helper.compose.rememberVectorPainterWithTint
import org.dweb_browser.pure.http.ext.FetchHook
import org.dweb_browser.pure.image.compose.ImageLoadResult
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import squircleshape.SquircleShape

data class AppIconOptions(
  val iconMaskable: Boolean = false,
  val iconMonochrome: Boolean = false,
)

/**
 * 应用图标的渲染标准
 *
 * 可以用在任何需要出现“图标”的地方
 */
@Composable
fun AppIcon(
  icon: ImageLoadResult?,
  modifier: Modifier = Modifier,
  color: Color = LocalContentColor.current,
  containerAlpha: Float? = null,
  containerColor: Color? = null,
  containerShadow: Dp? = null,
  iconShape: Shape? = null,
  iconPlaceholder: Painter? = null,
  iconError: Painter? = null,
  iconMaskable: Boolean = false,
  iconMonochrome: Boolean = false,
  iconDescription: String = "icon",
  iconFetchHook: FetchHook? = null,
) {
  AppIconOuter(
    iconSrc = icon,
    modifier = modifier,
    color = color,
    containerAlpha = containerAlpha,
    containerColor = containerColor,
    containerShadow = containerShadow,
    iconShape = iconShape,
    iconPlaceholder = iconPlaceholder,
    iconError = iconError,
    iconMaskable = iconMaskable,
    iconMonochrome = iconMonochrome,
    iconDescription = iconDescription,
    iconFetchHook = iconFetchHook,
  )
}

/**
 * 应用图标的渲染标准
 *
 * 可以用在任何需要出现“图标”的地方
 */
@Composable
fun AppIcon(
  iconUrl: String?,
  modifier: Modifier = Modifier,
  color: Color = LocalContentColor.current,
  containerAlpha: Float? = null,
  containerColor: Color? = null,
  iconShape: Shape? = null,
  iconPlaceholder: Painter? = null,
  iconError: Painter? = null,
  iconMaskable: Boolean = false,
  iconMonochrome: Boolean = false,
  iconDescription: String = "icon",
  iconFetchHook: FetchHook? = null,
) {
  AppIconOuter(
    iconSrc = iconUrl,
    modifier = modifier,
    color = color,
    containerAlpha = containerAlpha,
    containerColor = containerColor,
    iconShape = iconShape,
    iconPlaceholder = iconPlaceholder,
    iconError = iconError,
    iconMaskable = iconMaskable,
    iconMonochrome = iconMonochrome,
    iconDescription = iconDescription,
    iconFetchHook = iconFetchHook,
  )
}

@Composable
fun AppIcon(
  iconResource: StrictImageResource, modifier: Modifier = Modifier,
  color: Color = LocalContentColor.current,
  containerColor: Color? = null,
  iconShape: Shape? = null,
  iconPlaceholder: Painter? = null,
  iconError: Painter? = null,
  iconDescription: String = "icon",
  iconFetchHook: FetchHook? = null,
) {
  AppIconOuter(
    iconSrc = iconResource.src,
    modifier = modifier,
    color = color,
    containerColor = containerColor,
    iconShape = iconShape,
    iconPlaceholder = iconPlaceholder,
    iconError = iconError,
    iconMaskable = iconResource.purpose.contains(ImageResourcePurposes.Maskable),
    iconMonochrome = iconResource.purpose.contains(ImageResourcePurposes.Monochrome),
    iconDescription = iconDescription,
    iconFetchHook = iconFetchHook,
  )
}

@Composable
private fun AppIconOuter(
  iconSrc: Any?,
  modifier: Modifier,
  color: Color,
  containerAlpha: Float? = null,
  containerColor: Color?,
  containerShadow: Dp? = null,
  iconShape: Shape? = null,
  iconPlaceholder: Painter?,
  iconError: Painter?,
  iconMaskable: Boolean,
  iconMonochrome: Boolean,
  iconDescription: String,
  iconFetchHook: FetchHook?,
) {
  var isSuccess by remember { mutableStateOf(false) }
  // 如果没有提供背景色，那么就根据图标颜色进行自适应显示显示黑色或者白色的底色
  val safeContainerColor = containerColor
    ?: (if (color.luminance() > 0.5f) Color.Black else Color.White).copy(
      alpha = containerAlpha ?: 0.2f
    )
  val containerShape = iconShape ?: SquircleShape()
  // 只有加载成功的时候，才会显示背景裁切图
  var containerModifier = modifier
  containerShadow?.also { size ->
    containerModifier = containerModifier.shadow(size, containerShape)
  }
  containerModifier = containerModifier.background(safeContainerColor, containerShape)
  if (!isSuccess) {
    containerModifier = containerModifier.alpha(0.5f)
  }
  BoxWithConstraints(
    modifier = containerModifier.aspectRatio(1f),
    contentAlignment = Alignment.Center
  ) {
    val icon = when (iconSrc) {
      is String -> {
        PureImageLoader.SmartLoad(iconSrc, maxWidth, maxHeight, iconFetchHook)
      }

      is ImageLoadResult -> {
        iconSrc
      }

      null -> ImageLoadResult(busy = "no icon")

      else -> throw Exception("Invalid icon src type: $iconSrc")
    }
    isSuccess = icon.isSuccess
    AppIconInner(
      icon = icon,
      color = color,
      iconPlaceholder = iconPlaceholder,
      iconError = iconError,
      iconMaskable = iconMaskable,
      iconMonochrome = iconMonochrome,
      iconDescription = iconDescription,
    )
  }
}

@Composable
private fun AppIconInner(
  icon: ImageLoadResult,
  color: Color = LocalContentColor.current,
  iconPlaceholder: Painter? = null,
  iconError: Painter? = null,
  iconMaskable: Boolean = false,
  iconMonochrome: Boolean = false,
  iconDescription: String = "icon",
) {
  var iconModifier = Modifier.fillMaxSize();
  icon.with(onBusy = { // busyInfo ->
//    val painter = iconPlaceholder ?: rememberVectorPainterWithTint(
//      image = Icons.TwoTone.Image,
//      tintColor = color,
//    )
//    Image(painter, contentDescription = busyInfo, iconModifier)
  }, onError = { error ->
    val painter = iconError ?: rememberVectorPainterWithTint(
      image = Icons.TwoTone.BrokenImage,
      tintColor = color.copy(alpha = 0.5f),
    )
    Image(painter, contentDescription = error.message, iconModifier)
  }) { imageBitmap ->
    // 如果不可裁切，对图标进行一定的缩小。或者加载失败是，显示占位符，这时候不会显示遮罩（遮罩只为正确显示的图标服务），所以使用占位图，这时候就完整显示。
    val iconSize = if (iconMaskable) 1f else 0.87f
    iconModifier = iconModifier.scale(iconSize)
    /// 如果是单色的，那么将它作为图标进行展示，并跟随主题色
    if (iconMonochrome) {
      Icon(
        imageBitmap,
        contentDescription = iconDescription,
        modifier = iconModifier,
        tint = color,
      )
    } else {
      Image(
        imageBitmap,
        contentDescription = iconDescription,
        modifier = iconModifier,
      )
    }
  }
}
