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
actual fun CoilAsyncImage(
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
  if (model is String && model.startsWith("data:image")) {
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