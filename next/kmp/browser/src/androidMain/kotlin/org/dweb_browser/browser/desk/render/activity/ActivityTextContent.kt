package org.dweb_browser.browser.desk.render.activity

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.model.ActivityItem
import org.dweb_browser.browser.desk.model.ActivityItemRenderProp
import org.dweb_browser.sys.window.render.AppIconContainer

@Composable
internal fun ActivityTextContent(
  renderProp: ActivityItemRenderProp,
  content: ActivityItem.TextContent,
  innerPaddingDp: Dp,
  modifier: Modifier = Modifier,
) {
  val p2 = renderProp.detailAni.value
  Box(
    modifier.then(
      when {
        renderProp.showDetail -> Modifier.size(width = 240.dp, height = 64.dp)
        else -> Modifier.size(width = 120.dp, height = 32.dp)
      }
    ),
    contentAlignment = Alignment.CenterEnd,
  ) {
    if (!renderProp.showDetail || renderProp.detailAni.isRunning) {
      Text(
        text = content.shortText,
        color = Color.White,
        style = MaterialTheme.typography.bodySmall,
        modifier = when {
          renderProp.detailAni.isRunning -> {
            val blur = (8 * p2).coerceAtLeast(0f).dp
            Modifier
              .blur(blur)
              //.padding(8.dp)
              .graphicsLayer {
                translationY = -12 * density * p2
                alpha = (1 - p2).coerceAtLeast(0f)
              }
          }

          else -> Modifier
        }.padding(innerPaddingDp),
      )
    }
    if (renderProp.showDetail || renderProp.detailAni.isRunning) {
      Text(
        text = content.fullText,
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
        modifier = when {
          renderProp.detailAni.isRunning -> {
            val p3 = 1 - p2
            val blur = (8 * p3).coerceAtLeast(0f).dp
            Modifier
              .blur(blur)
              .graphicsLayer {
                translationY = 12 * density * p3
                alpha = (1 - p3).coerceAtLeast(0f)
              }
          }

          else -> Modifier
        }.padding(innerPaddingDp),
      )
    }
  }
}

@Preview
@Composable
fun ActivityTextContentPreview() {
  val renderProp = remember {
    ActivityItemRenderProp()
  }
  ActivityItemContentEffect(renderProp)
  val content = ActivityItem.TextContent("Hello", "Hello Gaubee")
  Box(
    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
  ) {
    Box(modifier = Modifier
      .padding(8.dp)
      .clip(AppIconContainer.defaultShape)
      .background(Color.Black)
      .clickable {
        renderProp.showDetail = !renderProp.showDetail
      }) {
      ActivityTextContent(renderProp = renderProp, content = content, innerPaddingDp = 8.dp)
    }
  }
}