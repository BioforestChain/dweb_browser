package info.bagen.dwebbrowser.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import info.bagen.dwebbrowser.R

@Composable
fun ListItemDeleteView(
  height: Dp = 56.dp,
  onClick: (() -> Unit)? = null,
  onDelete: (() -> Unit)? = null,
  enableExpand: Boolean = true, // 如果为 true，最右边会显示一个图标
  expandContent: (@Composable ColumnScope.() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit
) {
  var offsetX by remember { mutableStateOf(0f) }
  val heightPx = with(LocalDensity.current) { height.toPx() }
  val clickEnable by remember {
    derivedStateOf {
      onClick != null && offsetX != -heightPx
    }
  }
  val expand: MutableState<Boolean> = remember{ mutableStateOf(false) }

  Column {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(height)
    ) {
      Box(
        modifier = Modifier
          .height(height)
          .align(Alignment.CenterEnd)
          .background(Color.Red)
          .clickable(enabled = onDelete != null) { onDelete?.let { it() } }
      ) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete",
          modifier = Modifier
            .size(height)
            .padding(10.dp)
            .graphicsLayer(
              scaleY = offsetX / heightPx, scaleX = offsetX / heightPx, rotationZ = 180f
            )
        )
      }

      Row(modifier = Modifier
        .fillMaxWidth()
        .height(height)
        .offset {
          IntOffset(offsetX.roundToInt(), 0)
        }
        .draggable(
          state = rememberDraggableState { delta ->
            if (expand.value) return@rememberDraggableState // 如果是展开状态，不可滑动
            if (delta < 0 && offsetX <= -heightPx) {
              offsetX = -heightPx
            } else if (delta > 0 && offsetX >= 0) {
              offsetX = 0f
            } else {
              offsetX += delta
            }
          }, orientation = Orientation.Horizontal,
          onDragStopped = {
            offsetX = if (offsetX > (-heightPx / 2)) {
              0f
            } else {
              -heightPx
            }
          }
        )
        .background(MaterialTheme.colorScheme.surface)
        .clickable(enabled = clickEnable) { onClick?.let { it() } },
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .height(height)
            .padding(horizontal = 10.dp)
        ) { content() }
        if (enableExpand && expandContent != null) {
          Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_more),
            contentDescription = "expand",
            modifier = Modifier
              .size(height)
              .clickable { if (offsetX.roundToInt() == 0) expand.value = !expand.value }
              .padding(10.dp)
              .graphicsLayer(rotationZ = if (expand.value) 180f else 0f)
          )
        }
      }
    }
    if (expand.value) {
      Column(modifier = Modifier.fillMaxWidth()) {
        expandContent?.let { it() }
      }
    }
    Divider()
  }
}