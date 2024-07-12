package org.dweb_browser.pure.image.compose

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import coil3.Image
import coil3.annotation.ExperimentalCoilApi
import coil3.toBitmap

@OptIn(ExperimentalCoilApi::class)
fun Image.skikoToImageBitmap(): ImageBitmap {
  return toBitmap().asComposeImageBitmap()
}