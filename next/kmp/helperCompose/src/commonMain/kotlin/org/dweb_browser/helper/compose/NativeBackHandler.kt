package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable


@Composable
/**监听窗口的聚焦和失焦事件，聚焦的时候enabled为true，失去焦点的时候enabled为false*/
expect fun NativeBackHandler(enabled: Boolean = true, onBack: () -> Unit)
