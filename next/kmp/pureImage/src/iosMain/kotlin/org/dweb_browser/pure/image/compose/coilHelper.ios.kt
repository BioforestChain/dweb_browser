package org.dweb_browser.pure.image.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.Image
import coil3.annotation.ExperimentalCoilApi

@OptIn(ExperimentalCoilApi::class)
actual fun Image.toImageBitmap(): ImageBitmap {
  return asBitmap().asComposeImageBitmap()
}