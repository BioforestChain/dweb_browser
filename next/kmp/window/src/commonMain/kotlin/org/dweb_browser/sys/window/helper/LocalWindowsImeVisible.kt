package org.dweb_browser.sys.window.helper

import androidx.compose.runtime.mutableStateOf
import org.dweb_browser.helper.compose.compositionChainOf

val LocalWindowsImeVisible =
  compositionChainOf("WindowsImeVisible") { mutableStateOf(false) } // 由于小米手机键盘收起会有异常，所以自行维护键盘的显示和隐藏