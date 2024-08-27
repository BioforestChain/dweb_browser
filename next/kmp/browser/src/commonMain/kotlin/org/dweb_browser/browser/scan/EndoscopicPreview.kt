package org.dweb_browser.browser.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.sys.window.core.LocalWindowController

///内窥镜渲染模块

/**内窥镜视图*/
@Composable
expect fun EndoscopicPreview(modifier: Modifier, controller: SmartScanController)


@Composable
fun SmartScanController.EndoscopicPreview(modifier: Modifier) {
  LocalWindowController.current.navigation.GoBackHandler {
    updatePreviewType(SmartModuleTypes.Scanning)
  }
  EndoscopicPreview(modifier, this)
}

