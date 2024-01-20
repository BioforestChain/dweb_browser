package org.dweb_browser.dwebview

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun IDWebView.Render(
  modifier: Modifier = Modifier,
  onCreate: (suspend IDWebView.() -> Unit)? = null,
  onDispose: (suspend IDWebView.() -> Unit)? = null,
)