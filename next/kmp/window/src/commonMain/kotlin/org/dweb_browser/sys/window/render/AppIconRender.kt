package org.dweb_browser.sys.window.render


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BrokenImage
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import org.dweb_browser.helper.ImageResourcePurposes
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.helper.compose.rememberVectorPainterWithTint
import org.dweb_browser.pure.image.compose.ImageLoadResult
import org.dweb_browser.pure.image.compose.ImageLoader2
import org.dweb_browser.pure.image.offscreenwebcanvas.FetchHook
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
  containerColor: Color? = null,
  iconPlaceholder: Painter? = null,
  iconError: Painter? = null,
  iconMaskable: Boolean = false,
  iconMonochrome: Boolean = false,
  iconDescription: String = "icon",
  iconLoader: ImageLoader2? = null,
  iconFetchHook: FetchHook? = null,
) {
  AppIconOuter(
    iconSrc = icon,
    modifier = modifier,
    color = color,
    containerColor = containerColor,
    iconPlaceholder = iconPlaceholder,
    iconError = iconError,
    iconMaskable = iconMaskable,
    iconMonochrome = iconMonochrome,
    iconDescription = iconDescription,
    iconLoader = iconLoader,
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
  icon: String?,
  modifier: Modifier = Modifier,
  color: Color = LocalContentColor.current,
  containerColor: Color? = null,
  iconPlaceholder: Painter? = null,
  iconError: Painter? = null,
  iconMaskable: Boolean = false,
  iconMonochrome: Boolean = false,
  iconDescription: String = "icon",
  iconLoader: ImageLoader2? = null,
  iconFetchHook: FetchHook? = null,
) {
  AppIconOuter(
    iconSrc = icon,
    modifier = modifier,
    color = color,
    containerColor = containerColor,
    iconPlaceholder = iconPlaceholder,
    iconError = iconError,
    iconMaskable = iconMaskable,
    iconMonochrome = iconMonochrome,
    iconDescription = iconDescription,
    iconLoader = iconLoader,
    iconFetchHook = iconFetchHook,
  )
}

@Composable
fun AppIcon(
  iconResource: StrictImageResource, modifier: Modifier = Modifier,
  color: Color = LocalContentColor.current,
  containerColor: Color? = null,
  iconPlaceholder: Painter? = null,
  iconError: Painter? = null,
  iconDescription: String = "icon",
  iconLoader: ImageLoader2? = null,
  iconFetchHook: FetchHook? = null,
) {
  AppIconOuter(
    iconSrc = iconResource.src,
    modifier = modifier,
    color = color,
    containerColor = containerColor,
    iconPlaceholder = iconPlaceholder,
    iconError = iconError,
    iconMaskable = iconResource.purpose.contains(ImageResourcePurposes.Maskable),
    iconMonochrome = iconResource.purpose.contains(ImageResourcePurposes.Monochrome),
    iconDescription = iconDescription,
    iconLoader = iconLoader,
    iconFetchHook = iconFetchHook,
  )
}

@Composable
private fun AppIconOuter(
  iconSrc: Any?,
  modifier: Modifier,
  color: Color,
  containerColor: Color?,
  iconPlaceholder: Painter?,
  iconError: Painter?,
  iconMaskable: Boolean,
  iconMonochrome: Boolean,
  iconDescription: String,
  iconLoader: ImageLoader2? = null,
  iconFetchHook: FetchHook? = null,
) {
  val loader2 = iconLoader ?: ImageLoader2.GetImageLoader2()

  BoxWithConstraints {
    val icon = when (iconSrc) {
      is String -> {
        loader2.Load(iconSrc, maxWidth, maxHeight, iconFetchHook)
      }

      is ImageLoadResult -> {
        iconSrc
      }

      null -> ImageLoadResult(busy = "no icon")

      else -> throw Exception("Invalid icon src type: $iconSrc")
    }
    // 如果没有提供背景色，那么就根据图标颜色进行自适应显示显示黑色或者白色的底色
    val safeContainerColor = containerColor
      ?: (if (color.luminance() > 0.5f) Color.Black else Color.White).copy(alpha = 0.2f)
    // 只有加载成功的时候，才会显示背景裁切图
    val containerModifier = if (icon.isSuccess) {
      modifier
        .clip(SquircleShape())
        .background(safeContainerColor)
    } else {
      modifier
    }
    Box(modifier = containerModifier, contentAlignment = Alignment.Center) {
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
  icon.with(onBusy = { busyInfo ->
    val painter = iconPlaceholder ?: rememberVectorPainterWithTint(
      image = Icons.TwoTone.Image,
      tintColor = color,
    )
    Image(painter, contentDescription = busyInfo, iconModifier)
  }, onError = { error ->
    val painter = iconError ?: rememberVectorPainterWithTint(
      image = Icons.TwoTone.BrokenImage,
      tintColor = color,
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
