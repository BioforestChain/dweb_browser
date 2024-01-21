package org.dweb_browser.browser.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun AsyncImage(
  model: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
)