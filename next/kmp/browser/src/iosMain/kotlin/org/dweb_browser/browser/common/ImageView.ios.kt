package org.dweb_browser.browser.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import org.dweb_browser.helper.compose.LocalImageLoader

@Composable
actual fun AsyncImage(
  model: String,
  contentDescription: String?,
  modifier: Modifier,
  alignment: Alignment,
  contentScale: ContentScale,
  alpha: Float,
  colorFilter: ColorFilter?,
  filterQuality: FilterQuality,
) {
  val imageLoader = LocalImageLoader.current
  BoxWithConstraints(modifier) {
    val imageBitmap = imageLoader.Load(model, maxWidth, maxHeight)
    imageBitmap.with {
      Image(it, contentDescription = model, modifier = Modifier.fillMaxSize())
    }
  }
}