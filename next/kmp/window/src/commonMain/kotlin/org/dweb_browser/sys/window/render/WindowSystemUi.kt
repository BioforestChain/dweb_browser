package org.dweb_browser.sys.window.render

import androidx.compose.runtime.Composable


@Composable
expect fun NativeBackHandler(enabled: Boolean = true, onBack: () -> Unit)
