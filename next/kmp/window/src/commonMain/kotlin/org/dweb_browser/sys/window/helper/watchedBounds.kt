package org.dweb_browser.sys.window.helper

import androidx.compose.runtime.Composable
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.constant.WindowPropertyKeys

@Composable
fun WindowController.watchedBounds() = watchedState(watchKey = WindowPropertyKeys.Bounds) { bounds }