package org.dweb_browser.helper.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


// TODO 由于版本androidx.compose 升级为 1.2.0-beta1 但是jetpack-compose版本没有出来，临时增加
@Composable
expect fun CommonSwipeDismiss(
  modifier: Modifier = Modifier,
  background: @Composable RowScope.() -> Unit,
  onRemove: () -> Unit,
  content: @Composable RowScope.() -> Unit,
)

@Composable
fun ListSwipeItem(
  modifier: Modifier = Modifier,
  onRemove: () -> Unit,
  background: @Composable RowScope.() -> Unit = {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
  },
  content: @Composable RowScope.() -> Unit,
) {
  CommonSwipeDismiss(
    modifier, background, onRemove, content
  )
}

