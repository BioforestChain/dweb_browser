package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun Modifier.pointerActions(
  onHoverStart: () -> Unit = {},
  onHoverEnd: () -> Unit = {},
  onDoubleTap: () -> Unit = {},
  onMenu: () -> Unit = {},
  onTap: () -> Unit = {},
): Modifier