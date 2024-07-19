package org.dweb_browser.browser.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun CameraPreviewRender(
  modifier: Modifier = Modifier,
  controller: SmartScanController
)