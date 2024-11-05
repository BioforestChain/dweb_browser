package org.dweb_browser.sys.window.render

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.ImageResourcePurposes
import org.dweb_browser.helper.ImageResourceSize
import org.dweb_browser.helper.StrictImageResource
import org.dweb_browser.pure.http.ext.FetchHook
import org.dweb_browser.pure.image.compose.ImageLoadResult
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import squircleshape.CornerSmoothing
import squircleshape.SquircleShape

data class AppIcon(val container: AppIconContainer, val logo: AppLogo) {
  companion object {
    @Composable
    fun from(
      logo: AppLogo,
      containerBase: AppIconContainer? = null,
    ) = AppIcon(AppIconContainer.from(logo, containerBase), logo)
  }

  @Composable
  fun Render(
    modifier: Modifier = Modifier,
    logoModifier: Modifier = Modifier,
    innerContent: (@Composable () -> Unit)? = null,
  ) {
    container.Render(modifier) { logoModifierBase ->
      logo.Render(logoModifierBase.fillMaxSize().then(logoModifier))
      innerContent?.invoke()
    }
  }
}

data class AppIconContainer(
  val color: Color? = null,
  val brush: Brush? = null,
  val alpha: Float? = null,
  val shadow: Dp? = null,
  val shape: Shape? = null,
) {
  val safeShape get() = shape ?: defaultShape
  val safeAlpha get() = alpha ?: 1f

  companion object {
    val defaultShape = SquircleShape(30, CornerSmoothing.Small)

    fun backgroundColorFor(contentColor: Color) =
      (if (contentColor.luminance() > 0.5f) Color.Black else Color.White)

    @Composable
    fun from(
      logo: AppLogo,
      base: AppIconContainer? = null,
    ): AppIconContainer {

      /**
       * 如果没有提供背景色，那么就根据图标颜色进行自适应显示显示黑色或者白色的底色
       */
      val containerColor =
        base?.color ?: (logo.color ?: LocalContentColor.current).let { logoColor ->
          remember(
            logoColor
          ) { backgroundColorFor(logoColor) }
        }
      return base?.copy(color = containerColor) ?: AppIconContainer(color = containerColor)
    }
  }

  fun alpha(alpha: Float) = when (color) {
    null -> this
    else -> copy(color = color.copy(alpha = alpha))
  }

  fun withColorAndAlpha(color: Color? = null, alpha: Float? = null) = copy(color = color, alpha = alpha)

  @Composable
  fun Render(
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
  ) {
    val container = this
    Box(modifier.iconContainer(container), contentAlignment = Alignment.Center) {
      content(Modifier.clip(container.safeShape))
    }
  }
}

fun Modifier.iconContainer(options: AppIconContainer): Modifier = with(options) {
  this@iconContainer.aspectRatio(1f).let { modifier ->
    when (shadow) {
      null -> modifier
      else -> modifier.shadow(shadow, safeShape)
    }
  }.let { modifier ->
    when (color) {
      null -> modifier
      else -> modifier.background(alpha?.let { color.copy(alpha = it) } ?: color, safeShape)
    }
  }.let { modifier ->
    when (brush) {
      null -> modifier
      else -> modifier.background(brush, safeShape, alpha = safeAlpha)
    }
  }
}

data class AppLogo(
  val loadResult: ImageLoadResult? = null,
  val color: Color? = null,
  val placeholder: Painter? = null,
  val loadingContent: (@Composable (reason: String) -> Unit)? = null,
  val error: Painter? = null,
  val errorContent: (@Composable (error: Throwable?) -> Unit)? = null,
  val description: String? = null,
  val maskable: Boolean? = null,
  val monochrome: Boolean? = null,
) {
  val safeMaskable get() = maskable ?: false
  val safeMonochrome get() = monochrome ?: false
  val safeDescription get() = description ?: "logo"
  val safeColor @Composable get() = color ?: LocalContentColor.current

  fun withColor(color: Color? = null) = copy(color = color)

  companion object {
    val defaultSize = 64.dp
    val defaultDpSize = DpSize(defaultSize, defaultSize)

    @Composable
    fun fromUrl(
      url: String?,
      width: Dp? = null,
      height: Dp? = width,
      fetchHook: FetchHook? = null,
      base: AppLogo? = null,
      description: String? = null,
    ): AppLogo {
      if (url == null) return base ?: AppLogo()
      val loadResult =
        PureImageLoader.SmartLoad(
          url = url,
          maxWidth = width ?: defaultSize,
          maxHeight = height ?: defaultSize,
          currentColor = base?.color,
          hook = fetchHook
        )
      return base?.copy(loadResult = loadResult, description = description) ?: AppLogo(
        loadResult = loadResult, color = LocalContentColor.current, description = description
      )
    }

    private fun maxSize(sizes: List<ImageResourceSize>, density: Float) = when (val maxSize =
      sizes.maxByOrNull { it.height * it.width }
        ?.let { DpSize((it.width / density).dp, (it.height / density).dp) }) {
      null -> defaultDpSize
      else -> if (maxSize.width < defaultSize || maxSize.height < defaultSize) maxSize else defaultDpSize
    }

    @Composable
    fun from(
      resource: StrictImageResource?,
      width: Dp? = null,
      height: Dp? = width,
      fetchHook: FetchHook? = null,
      base: AppLogo? = null,
      description: String? = null,
    ): AppLogo {
      if (resource == null) return base ?: AppLogo()
      val size = if (width != null && height != null) {
        DpSize(width, height)
      } else {
        val maxSize = maxSize(resource.sizes, LocalDensity.current.density)
        if (width != null) {
          DpSize(width, width * (maxSize.height / maxSize.width))
        } else if (height != null) {
          DpSize(height * (maxSize.width / maxSize.height), height)
        } else maxSize
      }

      return fromUrl(resource.src, size.width, size.height, fetchHook, base, description).run {
        copy(
          maskable = maskable ?: resource.purpose.contains(ImageResourcePurposes.Maskable),
          monochrome = monochrome ?: resource.purpose.contains(ImageResourcePurposes.Monochrome)
        )
      }
    }

    @Composable
    fun pickFrom(
      resources: List<StrictImageResource>,
      width: Dp? = null,
      height: Dp? = width,
      fetchHook: FetchHook? = null,
      base: AppLogo? = null,
    ) = from(
      resource = remember(resources) { resources.pickLargest() },
      width,
      height,
      fetchHook,
      base
    )

    @Composable
    fun fromResources(
      resources: List<ImageResource>,
      width: Dp? = null,
      height: Dp? = width,
      fetchHook: FetchHook? = null,
      base: AppLogo? = null,
    ) = from(
      resource = remember(resources) { resources.toStrict().pickLargest() },
      width,
      height,
      fetchHook,
      base
    )
  }

  @Composable
  fun toIcon(containerBase: AppIconContainer? = null) = AppIcon.from(logo = this, containerBase)

  @Composable
  fun Render(modifier: Modifier = Modifier) {
    val logoOptions = this
    var iconModifier = modifier
    val errorRender: @Composable (Throwable?) -> Unit = { errorReason ->
      when (val errorContent = logoOptions.errorContent) {
        null -> logoOptions.error?.also { painter ->
          Box(iconModifier, contentAlignment = Alignment.Center) {
            Image(painter, contentDescription = logoOptions.safeDescription, Modifier.scale(0.62f))
          }
        }

        else -> errorContent(errorReason)
      }

    }
    logoOptions.loadResult?.with(onBusy = { busyReason ->
      when (val loadingContent = logoOptions.loadingContent) {
        null -> logoOptions.placeholder?.also { painter ->
          Box(iconModifier, contentAlignment = Alignment.Center) {
            Image(
              painter,
              contentDescription = logoOptions.safeDescription,
              iconModifier.scale(0.62f)
            )
          }
        }

        else -> loadingContent(busyReason)
      }
    }, onError = { errorReason ->
      errorRender(errorReason)
    }) { imageBitmap ->
      // 如果不可裁切，对图标进行一定的缩小。或者加载失败是，显示占位符，这时候不会显示遮罩（遮罩只为正确显示的图标服务），所以使用占位图，这时候就完整显示。
      val iconSize = if (logoOptions.safeMaskable) 1f else 0.87f
      iconModifier = iconModifier.scale(iconSize)
      /// 如果是单色的，那么将它作为图标进行展示，并跟随主题色
      if (logoOptions.safeMonochrome) {
        Icon(
          imageBitmap,
          contentDescription = logoOptions.safeDescription,
          modifier = iconModifier,
          tint = logoOptions.safeColor,
        )
      } else {
        Image(
          imageBitmap,
          contentDescription = logoOptions.safeDescription,
          modifier = iconModifier,
        )
      }
    } ?: errorRender(null)
  }
}