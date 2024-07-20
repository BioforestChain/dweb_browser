package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable

@Composable
actual fun NativeBackHandler(enabled: Boolean, onBack: () -> Unit) {
  androidx.activity.compose.BackHandler(enabled, onBack)
}