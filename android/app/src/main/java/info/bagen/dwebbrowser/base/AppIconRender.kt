package info.bagen.dwebbrowser.base


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.BrokenImage
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import coil.compose.rememberAsyncImagePainter
import org.dweb_browser.helper.android.rememberVectorPainterWithTint
import sv.lib.squircleshape.SquircleShape

/**
 * 应用图标的渲染标准
 *
 * 可以用在任何需要出现“图标”的地方
 */
@Composable
fun AppIconRender(
  modifier: Modifier,
  color: Color = LocalContentColor.current,
  containerColor: Color? = null,
  iconModel: Any? = null,
  iconPlaceholder: Painter? = null,
  iconMaskable: Boolean = false,
  iconMonochrome: Boolean = false,
  alwaysSuccess: Boolean = false
) {
  // 如果没有提供背景色，那么就根据图标颜色进行自适应显示显示黑色或者白色的底色
  val safeContainerColor = containerColor
    ?: (if (color.luminance() > 0.5f) Color.Black else Color.White).copy(alpha = 0.2f)

  var loadedSuccessIcon by remember {
    mutableStateOf(alwaysSuccess)
  }

  // 只有加载成功的时候，才会显示背景裁切图
  val containerModifier = if (loadedSuccessIcon) {
    Modifier
      .clip(SquircleShape())
      .background(safeContainerColor)
  } else {
    Modifier
  }
  Box(
    modifier = modifier.then(containerModifier),
  ) {
    val iconPainter = rememberAsyncImagePainter(
      model = iconModel,
      placeholder = iconPlaceholder ?: rememberVectorPainterWithTint(
        image = Icons.TwoTone.Image,
        tintColor = color,
      ),
      error = rememberVectorPainterWithTint(
        image = Icons.TwoTone.BrokenImage,
        tintColor = color,
      ),
      onError = { loadedSuccessIcon = alwaysSuccess },
      onLoading = { loadedSuccessIcon = alwaysSuccess },
      onSuccess = { loadedSuccessIcon = true },
    )
    // 如果不可裁切，对图标进行一定的缩小。或者加载失败是，显示占位符，这时候不会显示遮罩（遮罩只为正确显示的图标服务），所以使用占位图，这时候就完整显示。
    val iconSize = if (!loadedSuccessIcon || iconMaskable) 1f else 0.87f


    /// 如果是单色的，那么将它作为图标进行展示，并跟随主题色
    if (iconMonochrome) {
      Icon(
        iconPainter,
        contentDescription = "Window Icon",
        modifier = Modifier
          .align(Alignment.Center)
          .fillMaxSize()
          .scale(iconSize),
        tint = color,
      )
    } else {
      Image(
        iconPainter,
        contentDescription = "Window Icon",
        modifier = Modifier
          .align(Alignment.Center)
          .fillMaxSize()
          .scale(iconSize),
      )
    }
  }
}