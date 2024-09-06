package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import org.dweb_browser.helper.platform.LocalPureViewController
import org.dweb_browser.helper.platform.asDesktop


/**
 * TODO 临时方案处理弹出层的一些问题
 */
@Composable
actual fun ComposeWindowFocusOwnerEffect(show: Boolean, onDismiss: () -> Unit) {
  if (show) {
    val composeWindow by LocalPureViewController.current.asDesktop().composeWindowAsState()
    LaunchedEffect(composeWindow) {
      while (true) {
        delay(100)
        if (composeWindow.focusOwner != null) {
          onDismiss()
        }
      }
    }
  }
}
