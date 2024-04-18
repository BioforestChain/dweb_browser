package org.dweb_browser.pure.image.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

@Composable
fun SkikoCoilAsyncImage(
  model: Any?,
  contentDescription: String?,
  modifier: Modifier,
  transform: (AsyncImagePainter.State) -> AsyncImagePainter.State,
  onState: ((AsyncImagePainter.State) -> Unit)?,
  alignment: Alignment,
  contentScale: ContentScale,
  alpha: Float,
  colorFilter: ColorFilter?,
  filterQuality: FilterQuality,
  clipToBounds: Boolean,
) {
  // if (model is String && (model.endsWith(".svg") || model.endsWith(".webp") || model.startsWith("data:"))) {
  // TODO 目前发现 webp 使用 PureImageLoader 失败，无法显示。
  if (model is String && (model.endsWith(".svg") || model.startsWith("data:"))) {
    BoxWithConstraints(modifier) {
      PureImageLoader.SmartLoad(model, maxWidth, maxHeight).with {
        Image(it, contentDescription = model, modifier = modifier)
      }
    }
  } else {
    AsyncImage(
      model,
      contentDescription,
      modifier,
      transform,
      onState,
      alignment,
      contentScale,
      alpha,
      colorFilter,
      filterQuality,
      clipToBounds,
    )
  }
}