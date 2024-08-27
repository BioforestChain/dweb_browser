package org.dweb_browser.browser.desk.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberActivityStyle(): ActivityStyle = remember {
  ActivityStyle(
    centerWidth = 96f,
    openCenterWidth = 128f,
    overlayCutoutHeight = 0f,
    openOverlayCutoutHeight = 0f,
    screenMarginTop = 0f,
    openScreenMarginTop = 0f,
  )
}