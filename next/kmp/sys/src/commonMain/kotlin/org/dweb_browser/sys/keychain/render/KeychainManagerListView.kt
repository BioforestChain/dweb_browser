package org.dweb_browser.sys.keychain.render

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.compose.hoverCursor
import org.dweb_browser.sys.keychain.KeychainI18nResource
import org.dweb_browser.sys.keychain.KeychainManager
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.core.std.file.ext.blobFetchHook

@Composable
internal fun KeychainManager.ListView(
  modifier: Modifier,
  renderScope: WindowContentRenderScope,
) {
  val scope = rememberCoroutineScope()
  WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
    modifier = Modifier.fillMaxSize(),
    topBarTitleText = KeychainI18nResource.name(),
    topBarActions = {
      IconButton({ scope.launch { refreshList() } }) {
        Icon(Icons.TwoTone.Refresh, "refresh")
      }
    },
  ) { innerPadding ->
    Box(
      Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.Center
    ) {
      when (val mmList = microModuleList) {
        null -> CircularProgressIndicator(Modifier.size(64.dp))
        else -> when {
          mmList.isEmpty() -> Text(
            CommonI18n.no_data(),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alpha(0.5f)
          )

          else -> LazyColumn(Modifier.fillMaxSize()) {
            items(mmList, { it.mmid }) { mm ->
              ListItem(
                modifier = Modifier.hoverCursor().clickable {
                  openDetail(mm)
                },
                leadingContent = {
                  // TODO MicroModule Icon
                  when (val applicantIcon = remember {
                    mm.icons.toStrict().pickLargest()
                  }) {
                    null -> Icon(Icons.TwoTone.Image, contentDescription = "", Modifier.size(32.dp))
                    else -> Box(Modifier.size(32.dp)) {
                      AppIcon(applicantIcon, iconFetchHook = keychainRuntime.blobFetchHook)
                    }
                  }
                },
                headlineContent = { Text(mm.name) },
                supportingContent = { Text(mm.mmid) },
                trailingContent = {
                  Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, "go to details")
                },
              )
            }
          }
        }
      }
    }
  }
}