package org.dweb_browser.helper.compose

import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch


expect fun Modifier.cursorForHorizontalResize(): Modifier

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
              ).cursorForHorizontalResize().draggable(
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
            detailPane()
          }
        }
      }

      else -> {
        NavHost(
          navController = navigator.navHostController,
          startDestination = ListDetailPaneScaffoldRole.List.name,
          modifier = Modifier.fillMaxSize(),
          // 新页面进场
          enterTransition = { slideIn(iosTween(true)) { IntOffset(it.width, 0) } },
          // 新页面退场
          popExitTransition = { slideOut(iosTween(false)) { IntOffset(it.width, 0) } },

          // 旧页面退场
          exitTransition = { slideOut(iosTween(false)) { IntOffset(-it.width, 0) } },
          // 旧页面回场
          popEnterTransition = { slideIn(iosTween(true)) { IntOffset(-it.width, 0) } },
//          popExitTransition = { fadeOut() },
        ) {
          composable(ListDetailPaneScaffoldRole.List.name) {
            listPane()
          }
          composable(ListDetailPaneScaffoldRole.Detail.name) {
            detailPane()
          }
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
  val navHostController = rememberNavController()
  val scope = rememberCoroutineScope()
  return remember(navHostController, scope) {
    ListDetailPaneScaffoldNavigator(
      navHostController,
      scope
    )
  }
}

class ListDetailPaneScaffoldNavigator(
  internal val navHostController: NavHostController,
  private val scope: CoroutineScope,
) {
  var isFold by mutableStateOf(false)
    internal set
  internal var currentRole by mutableStateOf(ListDetailPaneScaffoldRole.List)
  fun navigateBack(onBack: () -> Unit = {}) {
    navigateTo(ListDetailPaneScaffoldRole.List, onDidLeave = onBack)
  }

  fun canNavigateBack(): Boolean {
    return currentRole !== ListDetailPaneScaffoldRole.List
  }

  private val navAniJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
    navHostController.currentBackStackEntryFlow.collect {
      println("QAQ currentBackStackEntryFlow ${it.destination.route}")
    }
  }


  fun navigateTo(
    role: ListDetailPaneScaffoldRole,
    onWillEnter: () -> Unit = {},
    onDidEnter: () -> Unit = {},
    onWillLeave: () -> Unit = {},
    onDidLeave: () -> Unit = {},
  ) {
    if (role == currentRole) {
      return
    }
    onWillEnter()
    onWillLeave()
    currentRole = role

    when (role) {
      ListDetailPaneScaffoldRole.List -> {
        println("QAQ navHostController.popBackStack list")
        navHostController.popBackStack()
      }

      ListDetailPaneScaffoldRole.Detail -> {
        println("QAQ navHostController.navigate detail")
        navHostController.navigate(role.name)
      }
    }
  }

  fun navigateToDetail() = navigateTo(ListDetailPaneScaffoldRole.Detail)
}
