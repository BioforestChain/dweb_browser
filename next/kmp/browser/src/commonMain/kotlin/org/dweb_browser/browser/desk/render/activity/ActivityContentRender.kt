package org.dweb_browser.browser.desk.render.activity

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.model.ActivityItemRenderProp
import org.dweb_browser.helper.compose.saveBlur

@Composable
fun ActivityItem.Content.Render(renderProp: ActivityItemRenderProp, modifier: Modifier = Modifier) {
  when (val content = this) {
    is ActivityItem.TextContent -> content.Render(renderProp, modifier)
  }
}

@Composable
fun ActivityItem.TextContent.Render(
  renderProp: ActivityItemRenderProp,
  modifier: Modifier = Modifier,
) {
  val p2 = renderProp.detailAni.value
  Box(
    modifier,
    contentAlignment = Alignment.Center,
  ) {
    val p3 = 1 - p2
    val blur = (8 * p3).coerceAtLeast(0f).dp
    Text(
      text = text,
      color = Color.White,
      style = MaterialTheme.typography.bodySmall,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = when {
        renderProp.detailAniFinished -> Modifier
        else -> Modifier
          .saveBlur(blur)
          .graphicsLayer {
            translationY = -8 * density * p3
            alpha = p2.coerceAtLeast(0f)
          }
      }
    )
  }
}
