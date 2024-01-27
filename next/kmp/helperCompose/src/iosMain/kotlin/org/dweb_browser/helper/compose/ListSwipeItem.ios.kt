package org.dweb_browser.helper.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DismissState
import androidx.compose.material3.DismissValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier

// TODO 由于版本androidx.compose 升级为 1.2.0-beta1 但是jetpack-compose版本没有出来，临时增加
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun CommonSwipeDismiss(
  modifier: Modifier,
  background: @Composable RowScope.() -> Unit,
  dismissContent: @Composable RowScope.() -> Unit,
  onRemove: () -> Unit
) {
  // val dismissState = rememberDismissState() // 不能用这个，不然会导致移除后remember仍然存在，显示错乱问题
  val dismissState = DismissState(
    initialValue = DismissValue.Default,
    confirmValueChange = { true },
    positionalThreshold = { density -> 56.0f * density },
  )
  LaunchedEffect(dismissState) {
    snapshotFlow { dismissState.currentValue }.collect {
      if (it != DismissValue.Default) {
        onRemove()
      }
    }
  }

  SwipeToDismiss(
    modifier = modifier,
    state = dismissState,
    background = background,
    dismissContent = dismissContent,
    directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart)
  )
}