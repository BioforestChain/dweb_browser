package org.dweb_browser.helper.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun ListDetailPaneScaffold(
  navigator: ListDetailPaneScaffoldNavigator,
  modifier: Modifier = Modifier,
  listPane: @Composable () -> Unit,
  detailPane: @Composable () -> Unit,
) {
  val minWidth = 360.dp
  BoxWithConstraints(modifier) {
    navigator.isFold = maxWidth.value >= 720
    val maxWidth = maxWidth - minWidth
    when {
      navigator.isFold -> {
        var listWidth by remember { mutableStateOf(minWidth) }
        val density = LocalDensity.current.density
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.fillMaxHeight().requiredWidth(listWidth).padding(end = 4.dp)) {
            listPane()
          }
          Box(
            Modifier.background(MaterialTheme.colorScheme.surfaceDim).padding(4.dp)
              .wrapContentWidth().fillMaxHeight(),
            contentAlignment = Alignment.Center,
          ) {
            var dragging by remember { mutableStateOf(false) }
            Box(
              Modifier.requiredWidth(6.dp).height(36.dp).background(
                when {
                  dragging -> MaterialTheme.colorScheme.surfaceTint
                  else -> MaterialTheme.colorScheme.surfaceBright
                }, shape = RoundedCornerShape(3.dp)
              ).hoverCursor(PointerIcon.HorizontalResize).draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                  listWidth += (delta / density).dp
                  if (listWidth < minWidth) {
                    listWidth = minWidth
                  }
                  if (listWidth > maxWidth) {
                    listWidth = maxWidth
                  }
                },
                onDragStarted = { dragging = true },
                onDragStopped = { dragging = false },
              ),
            )
          }
          Box(Modifier.fillMaxHeight().padding(start = 4.dp).weight(1f)) {
            remember(navigator.currentRole) {
              if (navigator.currentRole != ListDetailPaneScaffoldRole.Detail) {
                navigator.onDidLeaveDetail()
              }
            }
            detailPane()
          }
        }
      }

      else -> {
        val showList = navigator.currentRole == ListDetailPaneScaffoldRole.List
        AnimatedVisibility(
          showList,
          Modifier.fillMaxSize(),
          enter = SlideNavAnimations.popEnterTransition,
          exit = SlideNavAnimations.exitTransition,
        ) {
          DisposableEffect(null) {
            onDispose {
              if (navigator.currentRole != ListDetailPaneScaffoldRole.Detail) {
                navigator.onDidLeaveList()
              }
            }
          }
          listPane()
        }
        val showDetail = navigator.currentRole == ListDetailPaneScaffoldRole.Detail
        AnimatedVisibility(
          showDetail,
          modifier = Modifier.fillMaxSize(),
          enter = SlideNavAnimations.enterTransition,
          exit = SlideNavAnimations.popExitTransition,
        ) {
          DisposableEffect(null) {
            onDispose {
              if (navigator.currentRole != ListDetailPaneScaffoldRole.Detail) {
                navigator.onDidLeaveDetail()
              }
            }
          }
          detailPane()
        }
      }
    }
  }
}

enum class ListDetailPaneScaffoldRole {
  List, Detail,
}

@Composable
fun rememberListDetailPaneScaffoldNavigator(): ListDetailPaneScaffoldNavigator {
  return remember { ListDetailPaneScaffoldNavigator() }
}

class ListDetailPaneScaffoldNavigator(
) {
  var isFold by mutableStateOf(false)
    internal set
  internal var currentRole by mutableStateOf(ListDetailPaneScaffoldRole.List)

  fun canNavigateBack(): Boolean {
    return currentRole !== ListDetailPaneScaffoldRole.List
  }

  internal var onDidLeaveList by mutableStateOf({})
  internal var onDidLeaveDetail by mutableStateOf({})

  fun navigateTo(
    role: ListDetailPaneScaffoldRole,
    onDidLeaveCurrent: (() -> Unit)? = null,
    onDidLeaveRole: (() -> Unit)? = null,
  ) {
    if (role == currentRole) {
      return
    }
    currentRole = role
    when (role) {
      ListDetailPaneScaffoldRole.List -> {
        onDidLeaveCurrent?.also { onDidLeaveDetail ->
          this.onDidLeaveDetail = onDidLeaveDetail
        }
        onDidLeaveRole?.also { onDidLeaveList ->
          this.onDidLeaveList = onDidLeaveList
        }
      }

      ListDetailPaneScaffoldRole.Detail -> {
        onDidLeaveCurrent?.also { onDidLeaveList ->
          this.onDidLeaveList = onDidLeaveList
        }
        onDidLeaveRole?.also { onDidLeaveDetail ->
          this.onDidLeaveDetail = onDidLeaveDetail
        }
      }
    }
  }

  fun backToList(onDidLeaveDetail: (() -> Unit)? = null) {
    navigateTo(
      ListDetailPaneScaffoldRole.List,
      onDidLeaveCurrent = onDidLeaveDetail,
    )
  }

  fun navigateToDetail(onDidLeaveList: (() -> Unit)? = null) =
    navigateTo(
      ListDetailPaneScaffoldRole.Detail,
      onDidLeaveCurrent = onDidLeaveList,
    )
}
