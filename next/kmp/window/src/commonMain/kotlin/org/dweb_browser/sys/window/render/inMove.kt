package org.dweb_browser.sys.window.render

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.sys.window.core.WindowController

val inMoveStore = WeakHashMap<WindowController, MutableState<Boolean>>()

/**
 * 窗口是否在移动中
 */
val WindowController.inMove
  get() = inMoveStore.getOrPut(this) { mutableStateOf(false) }