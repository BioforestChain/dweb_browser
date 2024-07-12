package org.dweb_browser.pure.image.compose

import android.content.res.Resources
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import coil3.Image
import coil3.annotation.ExperimentalCoilApi
import coil3.asDrawable

@OptIn(ExperimentalCoilApi::class)
actual fun Image.toImageBitmap(): ImageBitmap {
  return asDrawable(resources = Resources.getSystem()).toBitmap().asImageBitmap()
}
