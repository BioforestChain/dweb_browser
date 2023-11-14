package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

@Composable
fun rememberPlatformViewController() = LocalPlatformViewController.current

val LocalPlatformViewController =
  compositionLocalOf<IPlatformViewController> { throw Exception("PlatformViewController no providers") }
