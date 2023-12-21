package org.dweb_browser.browser.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import org.dweb_browser.helper.android.getCoilImageLoader
import org.dweb_browser.browser.R

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
  val context = LocalContext.current
  coil.compose.AsyncImage(
    model = model,
    contentDescription = contentDescription,
    imageLoader = context.getCoilImageLoader(),
    modifier = modifier,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter,
    filterQuality = filterQuality,
    placeholder = painterResource(R.drawable.ic_launcher_foreground),
    error = painterResource(R.drawable.ic_launcher_foreground),
  )
}