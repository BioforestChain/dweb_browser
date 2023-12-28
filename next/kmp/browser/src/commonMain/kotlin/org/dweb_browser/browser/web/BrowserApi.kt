package org.dweb_browser.browser.web

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import org.dweb_browser.helper.ImageResource

expect fun ImageBitmap.toImageResource(): ImageResource?
expect fun getImageResourceRootPath(): String

// TODO 由于版本androidx.compose 升级为 1.2.0-beta1 但是jetpack-compose版本没有出来，临时增加
@Composable
expect fun CommonSwipeDismiss(
  background: @Composable RowScope.() -> Unit,
  dismissContent: @Composable RowScope.() -> Unit,
  modifier: Modifier = Modifier,
  onRemove: () -> Unit
)