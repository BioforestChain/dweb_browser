package org.dweb_browser.pure.image.compose

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.EqualityDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.asCoilImage
import coil3.compose.rememberAsyncImagePainter


@OptIn(ExperimentalCoilApi::class)
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
  modelEqualityDelegate: EqualityDelegate,
) {
  if (model is String && (model.endsWith(".svg") || model.endsWith(".webp"))) {
    val imageLoader = LocalCoilImageLoader.current
    BoxWithConstraints(modifier) {
      val imageBitmap = imageLoader.Load(model, maxWidth, maxHeight)
      imageBitmap.with(onError = {
        val webImageBitmap = LocalWebImageLoader.current.Load(model, maxWidth, maxHeight)
        webImageBitmap.with {
          Image(it, contentDescription = model, modifier = modifier)
        }
      }) {
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
      modelEqualityDelegate
    )
  }
}