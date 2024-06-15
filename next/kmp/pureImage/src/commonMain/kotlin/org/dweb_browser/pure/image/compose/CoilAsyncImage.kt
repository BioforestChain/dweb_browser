package org.dweb_browser.pure.image.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

@Composable
fun CoilAsyncImage(
  model: Any?,
  contentDescription: String?,
  imageLoader: ImageLoader,
  modifier: Modifier = Modifier,
  placeholder: Painter? = null,
  error: Painter? = null,
  fallback: Painter? = error,
  onLoading: ((AsyncImagePainter.State.Loading) -> Unit)? = null,
  onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null,
  onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
  alignment: Alignment = Alignment.Center,
  contentScale: ContentScale = ContentScale.Fit,
  alpha: Float = DefaultAlpha,
  colorFilter: ColorFilter? = null,
  filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
  clipToBounds: Boolean = true,
) = AsyncImage(
  model,
  contentDescription,
  imageLoader,
  modifier,
  placeholder,
  error,
  fallback,
  onLoading,
  onSuccess,
  onError,
  alignment,
  contentScale,
  alpha,
  colorFilter,
  filterQuality,
  clipToBounds,
)

@Composable
expect fun CoilAsyncImage(
  model: Any?,
  contentDescription: String?,
  modifier: Modifier = Modifier,
  transform: (AsyncImagePainter.State) -> AsyncImagePainter.State = AsyncImagePainter.DefaultTransform,
  onState: ((AsyncImagePainter.State) -> Unit)? = null,
  alignment: Alignment = Alignment.Center,
  contentScale: ContentScale = ContentScale.Fit,
  alpha: Float = DefaultAlpha,
  colorFilter: ColorFilter? = null,
  filterQuality: FilterQuality = DrawScope.DefaultFilterQuality,
  clipToBounds: Boolean = true,
)