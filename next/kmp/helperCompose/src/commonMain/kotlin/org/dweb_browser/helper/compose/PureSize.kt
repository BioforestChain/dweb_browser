package org.dweb_browser.helper.compose

import androidx.compose.ui.geometry.Size
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.PureBounds

@Serializable
data class PureSize(val width: Float = Float.NaN, val height: Float = Float.NaN) {
  fun toSize() = Size(width, height)
}

fun PureBounds.toPureSize() = PureSize(width = right - left, height = bottom - top)