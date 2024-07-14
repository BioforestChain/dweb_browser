package org.dweb_browser.sys.keychain.render


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material.icons.twotone.Key
import androidx.compose.material.icons.twotone.Password
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.dweb_browser.helper.base64String
import org.dweb_browser.helper.base64UrlString
import org.dweb_browser.helper.compose.ListDetailPaneScaffold
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.compose.rememberListDetailPaneScaffoldNavigator
import org.dweb_browser.helper.hexString
import org.dweb_browser.helper.utf8String
import org.dweb_browser.sys.keychain.KeychainI18nResource
import org.dweb_browser.sys.keychain.KeychainManager
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.blobFetchHook


@Composable
fun KeychainManager.Render(
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope,
) {
  val navigator = rememberListDetailPaneScaffoldNavigator()
  val win = LocalWindowController.current
  win.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.backToList {
      closeDetail()
    }
  }
  LaunchedEffect(detailController) {
    if (detailController != null) {
      navigator.navigateToDetail()
    } else {
      navigator.backToList()
    }
  }

  ListDetailPaneScaffold(
    modifier = modifier.withRenderScope(windowRenderScope),
    navigator = navigator,
    listPane = {
      ListView(Modifier, WindowContentRenderScope.Unspecified)
    },
    detailPane = {
      when (val detail = detailController) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(KeychainI18nResource.no_select_detail())
        }

        else -> BoxWithConstraints {
          detail.Render(
            Modifier.fillMaxSize(), WindowContentRenderScope.Unspecified
          )
        }
      }
    }
  )
}

@Composable
private fun KeychainManager.ListView(
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
            "暂无数据",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.alpha(0.5f)
          )

          else -> LazyColumn(Modifier.fillMaxSize()) {
            items(mmList, { it.mmid }) { mm ->
              ListItem(
                modifier = Modifier.clickable {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeychainManager.DetailManager.Render(
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
    Box(Modifier.padding(it), contentAlignment = Alignment.Center) {
      val allKeys by keysState.collectAsState()
      when (val keys = allKeys) {
        null -> CircularProgressIndicator(Modifier.size(64.dp))
        else -> LazyColumn(Modifier.fillMaxSize()) {
          items(keys, { key -> key }) { key ->
            var password by remember { mutableStateOf<ByteArray?>(null) }
            ListItem(
              leadingContent = {
                Icon(Icons.TwoTone.Key, null)
              },
              headlineContent = { Text(key) },
              trailingContent = {
                IconButton({
                  scope.launch { password = getPassword(key) }
                }) {
                  Icon(Icons.TwoTone.Password, "click to view password")
                }
              })

            password?.also { passwordSource ->
              BasicAlertDialog({ password = null }, Modifier.wrapContentSize()) {
                ElevatedCard {
                  Box(Modifier.zIndex(2f).align(Alignment.End)) {
                    IconButton({ password = null }) {
                      Icon(Icons.Default.Close, "close dialog")
                    }
                  }
                  PasswordView(passwordSource, Modifier.zIndex(1f))
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordView(passwordSource: ByteArray, modifier: Modifier = Modifier) {
  Column(modifier) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    PrimaryTabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
      PasswordViewMode.entries.forEachIndexed { tabIndex, tab ->
        Tab(
          selected = selectedTabIndex == tabIndex,
          onClick = {
            selectedTabIndex = tabIndex
          },
        ) {
          Text(tab.label(), modifier = Modifier.padding(8.dp))
        }
      }
    }
    Box(Modifier.weight(1f).padding(8.dp)) {
      when (val passwordViewMode = PasswordViewMode.entries[selectedTabIndex]) {
        PasswordViewMode.Utf8 -> Column {
          TextField(
            passwordSource.utf8String,
            {},
            readOnly = true
          )
          Row(Modifier.align(Alignment.End)) {
            Button({}, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
              Icon(Icons.TwoTone.ContentCopy, "copy")
              Text(KeychainI18nResource.password_copy())
            }
          }
        }

        PasswordViewMode.Base64 -> Column {
          var urlMode by remember { mutableStateOf(false) }
          TextField(
            if (urlMode) passwordSource.base64UrlString else passwordSource.base64String,
            {},
            readOnly = true
          )
          Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(
              checked = urlMode,
              onCheckedChange = {
                urlMode = it
              } // null recommended for accessibility with screenreaders
            )
            Text(text = KeychainI18nResource.password_base64_url_mode())
            Spacer(Modifier)
            Button({}, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
              Icon(Icons.TwoTone.ContentCopy, "copy")
              Text(KeychainI18nResource.password_copy())
            }
          }
        }

        PasswordViewMode.Binary -> Column {
          var hexMode by remember { mutableStateOf(true) }
          TextField(
            if (hexMode) passwordSource.hexString else passwordSource.joinToString(","),
            {},
            readOnly = true
          )
          Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Checkbox(
              checked = hexMode,
              onCheckedChange = {
                hexMode = it
              } // null recommended for accessibility with screenreaders
            )
            Text(text = KeychainI18nResource.password_binary_hex_mode())
            Spacer(Modifier)
            Button({}, contentPadding = ButtonDefaults.ButtonWithIconContentPadding) {
              Icon(Icons.TwoTone.ContentCopy, "copy")
              Text(KeychainI18nResource.password_copy())
            }
          }
        }
      }
    }
  }
}

enum class PasswordViewMode(val mode: String, val label: SimpleI18nResource) {
  Utf8("utf8", KeychainI18nResource.password_mode_label_utf8),
  Base64("base64", KeychainI18nResource.password_mode_label_base64),
  Binary("binary", KeychainI18nResource.password_mode_label_binary),
}