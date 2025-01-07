package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Modifier.pointerActions(
  onHoverStart: (() -> Unit)? = null,
  onHoverEnd: (() -> Unit)? = null,
  onDoubleTap: (() -> Unit)? = null,
  onMenu: (() -> Unit)? = null,
  onTap: (() -> Unit)? = null,
): Modifier