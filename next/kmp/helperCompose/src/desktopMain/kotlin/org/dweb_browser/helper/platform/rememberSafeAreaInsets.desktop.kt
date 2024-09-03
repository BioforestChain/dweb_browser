package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import org.dweb_browser.helper.PureBounds

@Composable
actual fun rememberSafeAreaInsets(): PureBounds = rememberSafeAreaInsetsCommon()
