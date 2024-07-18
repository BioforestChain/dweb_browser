package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.sys.keychain.KeychainManager
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun KeychainManager.DetailManager.Render(
  modifier: Modifier,
  renderScope: WindowContentRenderScope,
) {
  val scope = rememberCoroutineScope()
  renderScope.WindowContentScaffoldWithTitleText(
    modifier,
    topBarTitleText = manifest.name,
    topBarActions = {
      IconButton({ scope.launch { refresh() } }) {
        Icon(Icons.TwoTone.Refresh, "refresh")
      }
    },
  ) {
    Box(Modifier.fillMaxSize().padding(it), contentAlignment = Alignment.Center) {
      val allKeys by keysState.collectAsState()
      when (val keys = allKeys) {
        null -> CircularProgressIndicator(Modifier.size(64.dp))

        else -> when {
          keys.isEmpty() -> Text(
            CommonI18n.no_data(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alpha(0.5f)
          )

          else -> LazyColumn(Modifier.fillMaxSize()) {
            items(keys, { key -> key }) { key ->
              key.KeyItemView()
              HorizontalDivider()
            }
          }
        }
      }
    }
  }
}

