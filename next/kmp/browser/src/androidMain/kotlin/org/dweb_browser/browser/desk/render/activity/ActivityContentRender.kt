package org.dweb_browser.browser.desk.render.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.model.ActivityItemRenderProp
import org.dweb_browser.sys.window.render.AppIconContainer

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
        renderProp.detailAni.isRunning -> Modifier
          .blur(blur)
          .graphicsLayer {
            translationY = -8 * density * p3
            alpha = p2.coerceAtLeast(0f)
          }

        else -> Modifier
      }
    )
  }
}

@Preview
@Composable
fun ActivityTextContentPreview() {
  val renderProp = remember {
    ActivityItemRenderProp()
  }
  ActivityItemContentEffect(renderProp)
  val content = ActivityItem.TextContent("Hello Gaubee，这是一个长文本")
  Box(
    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
  ) {
    Box(modifier = Modifier
      .padding(16.dp)
      .clip(AppIconContainer.defaultShape)
      .background(Color.Black)
      .clickable {
        renderProp.showDetail = !renderProp.showDetail
      }) {
      content.Render(renderProp = renderProp)
    }
  }
}