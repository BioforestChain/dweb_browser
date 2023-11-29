package org.dweb_browser.browser.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import org.dweb_browser.helper.WARNING

@Composable
actual fun AsyncImage(
  model: Any?,
  contentDescription: String?,
  modifier: Modifier,
  alignment: Alignment,
  contentScale: ContentScale,
  alpha: Float,
  colorFilter: ColorFilter?,
  filterQuality: FilterQuality,
) {
  WARNING("Not yet implemented AsyncImage")
}