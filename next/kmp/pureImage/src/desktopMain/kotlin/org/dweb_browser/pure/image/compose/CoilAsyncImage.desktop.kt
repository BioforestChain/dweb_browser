package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
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
  SkikoCoilAsyncImage(
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