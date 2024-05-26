package org.dweb_browser.helper

import androidx.compose.ui.geometry.Rect

fun PureRect.toRect() = Rect(top = y, left = x, bottom = y + height, right = x + width)
