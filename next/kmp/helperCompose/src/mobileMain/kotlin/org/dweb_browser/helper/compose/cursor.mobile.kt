package org.dweb_browser.helper.compose

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon

actual fun Modifier.hoverCursor(cursor: PointerIcon): Modifier = this

actual val PointerIcon.Companion.HorizontalResize get() = PointerIcon.Default
actual val PointerIcon.Companion.VerticalResize get() = PointerIcon.Default
actual val PointerIcon.Companion.Move get() = PointerIcon.Default
actual val PointerIcon.Companion.Wait get() = PointerIcon.Default
