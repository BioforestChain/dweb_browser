package org.dweb_browser.browser.web.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.web.ui.dimenPageHorizontal

@Composable
internal fun PanelTitle(
  titleText: String,
  titleIcon: (@Composable () -> Unit)? = null,
  enabled: Boolean = true,
  trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
  Row(
    modifier = Modifier.padding(horizontal = dimenPageHorizontal)
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    FilterChip(
      onClick = {},
      enabled = enabled,
      selected = true,
      label = { Text(titleText) },
      leadingIcon = titleIcon,
    )
    trailingContent?.invoke(this)
  }
}