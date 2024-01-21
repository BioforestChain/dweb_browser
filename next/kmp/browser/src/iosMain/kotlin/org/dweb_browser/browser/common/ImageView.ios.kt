package org.dweb_browser.browser.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.pure.image.compose.LocalCoilImageLoader

@Composable
actual fun AsyncImage(
  model: String,
  contentDescription: String?,
  modifier: Modifier,
) {
  val imageLoader = LocalCoilImageLoader.current
  BoxWithConstraints(modifier) {
    val imageBitmap = imageLoader.Load(model, maxWidth, maxHeight)
    imageBitmap.with {
      Image(it, contentDescription = model, modifier = Modifier.fillMaxSize())
    }
  }
}