package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// TODO 由于版本androidx.compose 升级为 1.2.0-beta1 但是jetpack-compose版本没有出来，临时增加
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun CommonSwipeDismiss(
  modifier: Modifier,
  background: @Composable RowScope.() -> Unit,
  dismissContent: @Composable RowScope.() -> Unit,
  onRemove: () -> Unit
) {
  val dismissState = SwipeToDismissBoxState(
    initialValue = SwipeToDismissBoxValue.Settled,
    density = LocalDensity.current,
    confirmValueChange = { true },
    positionalThreshold = with(LocalDensity.current) { { 56.dp.toPx() } }
  )

  LaunchedEffect(dismissState) {
    snapshotFlow { dismissState.currentValue }.collect {
      if (it != SwipeToDismissBoxValue.Settled) {
        onRemove()
      }
    }
  }

  SwipeToDismissBox(
    state = dismissState,
    backgroundContent = background,
    modifier = modifier,
    enableDismissFromStartToEnd = true,
    enableDismissFromEndToStart = true,
    content = dismissContent
  )
}