package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import org.dweb_browser.helper.platform.PlatformViewController

@Composable
fun rememberPlatformViewController() =
  LocalPlatformViewController.current ?: _rememberPlatformViewController()


@Composable
internal expect fun _rememberPlatformViewController(): PlatformViewController

val LocalPlatformViewController = compositionLocalOf<PlatformViewController?> { null }
