package org.dweb_browser.sys.window.render

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.helper.compose.AutoResizeTextContainer
import org.dweb_browser.helper.compose.AutoSizeText
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.helper.LocalWindowLimits

/**
 * 身份渲染
 */
@Composable
fun WindowController.IdRender(
  modifier: Modifier = Modifier, contentColor: Color = LocalContentColor.current,
) {
  val minWidth = LocalWindowLimits.current.minWidth
  AutoResizeTextContainer(
    modifier.fillMaxHeight().widthIn(min = minWidth.dp)
  ) {
    val textStyle = MaterialTheme.typography.bodySmall
    AutoSizeText(text = incForRender,
      color = contentColor,
      style = textStyle,
      modifier = Modifier.align(Alignment.Center),
      overflow = TextOverflow.Visible,
      softWrap = false,
      onResize = { lightHeight = fontSize * 1.25f })
  }
}

/**
 * 用来窗口渲染的唯一标识
 */
val WindowController.incForRender get() = state.constants.owner