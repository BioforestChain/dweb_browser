package org.dweb_browser.browser.store.render

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.twotone.DeleteForever
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import org.dweb_browser.browser.store.StoreController
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.compose.ListDetailPaneScaffold
import org.dweb_browser.helper.compose.rememberListDetailPaneScaffoldNavigator
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.imageFetchHook

@Composable
private fun StoreController.loadProfileDetails(key1: Any? = null, onLoaded: suspend () -> Unit) =
  key(key1) {
    produceState(emptyList()) {
      value = dWebProfileStore.getAllProfileNames().mapNotNull { profileName ->
        println("QAQ profileName=$profileName mm=${
          storeNMM.bootstrapContext.dns.queryAll(
            profileName
          ).joinToString(",") { it.mmid }
        }")
        storeNMM.bootstrapContext.dns.queryAll(profileName).firstOrNull { it.mmid == profileName }
      }
      onLoaded()
    }
  }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreController.Render(modifier: Modifier, windowRenderScope: WindowContentRenderScope) {
  val navigator = rememberListDetailPaneScaffoldNavigator()
  var detailItem by remember { mutableStateOf<MicroModule?>(null) }
  remember(detailItem) {
    if (detailItem != null) {
      navigator.navigateToDetail { detailItem = null }
    }
  }
  LocalWindowController.current.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.backToList {
      detailItem = null
    }
  }


  ListDetailPaneScaffold(
    navigator = navigator,
    modifier = modifier.withRenderScope(windowRenderScope),
    listPane = {
      var refreshCount by remember { mutableStateOf(0) }
      WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
        Modifier.fillMaxSize(),
        topBarTitleText = "数据列表",
        topBarActions = {
          IconButton({ refreshCount += 1 }) {
            Icon(Icons.Default.Refresh, contentDescription = "refresh list")
          }
        }
      ) { paddingValues ->
        var isLoading by remember { mutableStateOf(true) }
        val profileDetails by loadProfileDetails(refreshCount) { isLoading = false }
        when {
          isLoading -> Box(
            Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
          ) {
            Text("数据加载中……")
          }

          else -> LazyColumn(Modifier.fillMaxSize().padding(paddingValues)) {
            items(profileDetails) { item ->
              ListItem(
                leadingContent = {
                  // TODO MicroModule Icon
                  when (val applicantIcon = remember {
                    item.icons.toStrict().pickLargest()
                  }) {
                    null -> Icon(Icons.TwoTone.Image, contentDescription = "", Modifier.size(32.dp))
                    else -> Box(Modifier.size(32.dp)) {
                      AppIcon(applicantIcon, iconFetchHook = storeNMM.imageFetchHook)
                    }
                  }
                },
                headlineContent = { Text(item.short_name) },
                supportingContent = { Text(item.mmid) },
                modifier = Modifier.clickable { detailItem = item })
            }
          }
        }
      }
    },
    detailPane = {
      when (val profileDetail = detailItem) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("请选择一项数据")
        }

        else ->
          WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
            Modifier.fillMaxSize(),
            topBarTitleText = "数据项详情:${profileDetail.short_name}"
          ) { paddingValues ->
            Column(Modifier.padding(paddingValues).fillMaxSize()) {
              key(profileDetail) {
                var forceDeleteProfile by remember { mutableStateOf(false) }
                var showForceDeleteProfileDialog by remember { mutableStateOf(false) }
                FilledTonalButton({
                  storeNMM.scopeLaunch(cancelable = false) {
                    if (!forceDeleteProfile && dWebProfileStore.isUsingProfile(profileDetail.mmid)) {
                      showForceDeleteProfileDialog = true
                    } else {
                      dWebProfileStore.deleteProfile(profileDetail.mmid)
                    }
                  }
                }) {
                  Text("清除数据")
                }

                if (showForceDeleteProfileDialog) {
                  var closeJob by remember { mutableStateOf<Job?>(null) }
                  AlertDialog(
                    { showForceDeleteProfileDialog = false },
                    title = { Text("程序正在运行") },
                    text = { Text("${profileDetail.short_name} 正在运行，如果要清除它的数据，需要先将它关停。") },
                    confirmButton = {
                      FilledTonalButton(
                        {
                          closeJob = storeNMM.scopeLaunch(cancelable = true) {
                            storeNMM.bootstrapContext.dns.close(profileDetail.mmid)
                            dWebProfileStore.deleteProfile(profileDetail.mmid)
                            showForceDeleteProfileDialog = false
                          }
                        },
                        enabled = closeJob == null,
                        colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.error)
                      ) {
                        Row {
                          when (closeJob) {
                            null -> Icon(
                              Icons.TwoTone.DeleteForever,
                              contentDescription = "kill and delete profile"
                            )

                            else -> CircularProgressIndicator(
                              modifier = Modifier.size(24.dp),
                              color = MaterialTheme.colorScheme.secondary,
                              trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                          }
                          Text("关停并清除")
                        }

                      }
                    },
                    dismissButton = {
                      Button({
                        closeJob?.cancel()
                        closeJob = null
                        showForceDeleteProfileDialog = false
                      }) {
                        Text("取消")
                      }
                    })
                }
              }
            }
          }
      }
    },
  )
}