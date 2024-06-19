package org.dweb_browser.helper.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp


expect fun Modifier.cursorForHorizontalResize(): Modifier

@Composable
fun ListDetailPaneScaffold(
  listPane: @Composable () -> Unit,
  detailPane: @Composable () -> Unit,
) {
  val minWidth = 360.dp
  BoxWithConstraints {
    val nav = LocalListDetailPaneScaffoldNavigator.current
    nav.isFold = maxWidth.value >= 720
    val maxWidth = maxWidth - minWidth
    when {
      nav.isFold -> {
        var listWidth by remember { mutableStateOf(minWidth) }
        val density = LocalDensity.current.density
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.fillMaxHeight().requiredWidth(listWidth).padding(end = 4.dp)) {
            listPane()
          }
          Box(
            Modifier.requiredSize(width = 6.dp, height = 36.dp)
              .background(
                //              MaterialTheme.colors.secondary.copy(alpha = 0.5f),
                Color.Red,
                shape = RoundedCornerShape(3.dp)
              )
              .cursorForHorizontalResize()
              .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                  listWidth += (delta / density).dp
                  if (listWidth < minWidth) {
                    listWidth = minWidth
                  }
                  if (listWidth > maxWidth) {
                    listWidth = maxWidth
                  }
                }
              ),
          )
          Box(Modifier.fillMaxHeight().padding(start = 4.dp).weight(1f)) {
            detailPane()
          }
        }
      }

      else -> {
        when (LocalListDetailPaneScaffoldNavigator.current.currentRole) {
          ListDetailPaneScaffoldRole.List -> listPane()
          ListDetailPaneScaffoldRole.Detail -> detailPane()
        }
      }
    }
  }
}

enum class ListDetailPaneScaffoldRole {
  List,
  Detail,
}

@Composable
fun rememberListDetailPaneScaffoldNavigator(): ListDetailPaneScaffoldNavigator {
  return LocalListDetailPaneScaffoldNavigator.current
}

class ListDetailPaneScaffoldNavigator {
  var isFold by mutableStateOf(false)
    internal set
  internal var currentRole by mutableStateOf(ListDetailPaneScaffoldRole.List)
  fun navigateBack() {
    currentRole = ListDetailPaneScaffoldRole.List
  }

  fun canNavigateBack(): Boolean {
    return currentRole !== ListDetailPaneScaffoldRole.List
  }

  fun navigateTo(role: ListDetailPaneScaffoldRole) {
    currentRole = role
  }
}

internal val LocalListDetailPaneScaffoldNavigator =
  compositionChainOf("ListDetailPaneScaffoldNavigator") { ListDetailPaneScaffoldNavigator() }
