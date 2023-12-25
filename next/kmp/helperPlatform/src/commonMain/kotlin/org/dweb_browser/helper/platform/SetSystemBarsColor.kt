package org.dweb_browser.helper.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
expect fun SetSystemBarsColor(bgColor: Color, fgColor: Color)