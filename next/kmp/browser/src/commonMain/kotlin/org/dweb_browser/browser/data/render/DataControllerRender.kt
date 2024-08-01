package org.dweb_browser.browser.data.render

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
import kotlinx.coroutines.launch
import org.dweb_browser.browser.data.DataController
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.compose.ListDetailPaneScaffold
import org.dweb_browser.helper.compose.rememberListDetailPaneScaffoldNavigator
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.core.WindowSurface
import org.dweb_browser.sys.window.core.helper.pickLargest
import org.dweb_browser.sys.window.core.helper.toStrict
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController

class ProfileDetail(val profileName: String, val mm: MicroModule) : IMicroModuleManifest by mm {

}

@Composable
private fun DataController.loadProfileDetails(onLoaded: suspend () -> Unit) =
  produceState(emptyList()) {
    value = dWebProfileStore.getAllProfileNames().mapNotNull { profileName ->
      storeNMM.bootstrapContext.dns.queryAll(profileName).firstOrNull { it.mmid == profileName }
        ?.let { mm -> ProfileDetail(profileName, mm) }
    }
    onLoaded()
  }

@Composable
fun DataController.Render(modifier: Modifier, windowRenderScope: WindowContentRenderScope) {
  val navigator = rememberListDetailPaneScaffoldNavigator()
  var currentDetailItem by remember { mutableStateOf<ProfileDetail?>(null) }
  remember(currentDetailItem) {
    if (currentDetailItem != null) {
      navigator.navigateToDetail { currentDetailItem = null }
    }
  }
  LocalWindowController.current.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.backToList {
      currentDetailItem = null
    }
  }

  var refreshCount by remember { mutableStateOf(0) }

  ListDetailPaneScaffold(
    navigator = navigator,
    modifier = modifier.withRenderScope(windowRenderScope),
    listPane = {
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
        val profileDetails by key(refreshCount) {
          loadProfileDetails {
            isLoading = false
          }
        }
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
                      AppIcon(applicantIcon, iconFetchHook = storeNMM.blobFetchHook)
                    }
                  }
                },
                headlineContent = { Text(item.short_name) },
                supportingContent = { Text(item.profileName) },
                modifier = Modifier.clickable { currentDetailItem = item })
            }
          }
        }
      }
    },
    detailPane = {
      when (val profileDetail = currentDetailItem) {
        null -> WindowContentRenderScope.Unspecified.WindowSurface {
          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("请选择一项数据")
          }
        }

        else ->
          WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
            Modifier.fillMaxSize(),
            topBarTitleText = "数据项详情:${profileDetail.short_name}"
          ) { paddingValues ->
            var deleteJob by remember { mutableStateOf<Job?>(null) }
            val deleteProfile: suspend (ProfileDetail) -> Unit = remember {
              { detail ->
                dWebProfileStore.deleteProfile(detail.mmid)
                refreshCount += 1
                if (currentDetailItem == detail) {
                  navigator.backToList()
                }
              }
            }
            Column(Modifier.padding(paddingValues).fillMaxSize()) {
              key(profileDetail) {
                var showForceDeleteProfileDialog by remember { mutableStateOf(false) }
                FilledTonalButton(
                  {
                    storeNMM.scopeLaunch(cancelable = false) {
                      if (storeNMM.bootstrapContext.dns.isRunning(profileDetail.mmid)) {
                        showForceDeleteProfileDialog = true
                      } else {
                        deleteJob = launch {
                          deleteProfile(profileDetail)
                        }
                      }
                    }
                  },
                  enabled = deleteJob == null,
                  colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    when (deleteJob) {
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
                    Text("清除数据")
                  }
                }

                if (showForceDeleteProfileDialog) {
                  AlertDialog(
                    { showForceDeleteProfileDialog = false },
                    title = { Text("程序正在运行") },
                    text = { Text("${profileDetail.short_name} 正在运行，如果要清除它的数据，需要先将它关停。") },
                    confirmButton = {
                      FilledTonalButton(
                        {
                          deleteJob = storeNMM.scopeLaunch(cancelable = true) {
                            storeNMM.bootstrapContext.dns.close(profileDetail.mmid)
                            deleteProfile(profileDetail)
                            showForceDeleteProfileDialog = false
                          }
                        },
                        enabled = deleteJob == null,
                        colors = ButtonDefaults.filledTonalButtonColors(contentColor = MaterialTheme.colorScheme.error),
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          when (deleteJob) {
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
                        deleteJob?.cancel()
                        deleteJob = null
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