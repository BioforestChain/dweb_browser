package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
expect fun NativeDialog(
  onCloseRequest: () -> Unit,
  properties: NativeDialogProperties = remember { NativeDialogProperties() },
  setContent: @Composable () -> Unit,
)

internal val DialogMinWidth = 280.dp
internal val DialogMaxWidth = 560.dp
internal val DialogMinHeight = 140.dp
internal val DialogMaxHeight = 800.dp

class NativeDialogProperties(
  val modal: Boolean = true,
  val title: String? = null,
  val icon: ImageBitmap? = null,
  val darkTheme: Boolean? = null,
  val minWidth: Dp = DialogMinWidth,
  val maxWidth: Dp = DialogMaxWidth,
  val minHeight: Dp = DialogMinHeight,
  val maxHeight: Dp = DialogMaxHeight,
)