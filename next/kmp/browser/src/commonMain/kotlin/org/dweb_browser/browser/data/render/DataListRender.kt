package org.dweb_browser.browser.data.render

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.twotone.DeleteForever
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material.icons.twotone.MoreHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.data.DataController
import org.dweb_browser.browser.data.DataI18n
import org.dweb_browser.core.std.file.ext.blobFetchHook
import org.dweb_browser.helper.compose.CommonI18n
import org.dweb_browser.helper.compose.SwipeToViewBox
import org.dweb_browser.helper.compose.rememberSwipeToViewBoxState
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.render.AppLogo

@Composable
fun DataController.ListRender(goToDetail: (DataController.ProfileDetail) -> Unit) {
  WindowContentRenderScope.Unspecified.WindowContentScaffoldWithTitleText(
    Modifier.fillMaxSize(),
    topBarTitleText = "数据列表",
    topBarActions = {
      IconButton({ refresh() }) {
        Icon(Icons.Default.Refresh, contentDescription = "refresh list")
      }
    }) { paddingValues ->
    var isLoading by remember { mutableStateOf(true) }
    var profileInfos by remember { mutableStateOf<List<DataController.ProfileInfo>>(emptyList()) }
    LaunchedEffect(isLoading, refreshFlow.collectAsState().value) {
      profileInfos = loadProfileInfos()
      isLoading = false
    }
    when {
      isLoading -> Box(
        Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center
      ) {
        Text("数据加载中……")
      }

      profileInfos.isEmpty() -> Box(
        Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center
      ) {
        Text("暂无数据")
      }

      else -> LazyColumn(Modifier.fillMaxSize().padding(paddingValues)) {
        itemsIndexed(
          profileInfos,
          key = { _, profileInfo -> profileInfo.profileName.key },
        ) { index, profileInfo ->
          val mmidText: @Composable () -> AnnotatedString = {
            profileInfo.profileName.profile?.let { alias ->
              buildAnnotatedString {
                append(profileInfo.mmid)
                withStyle(LocalTextStyle.current.toSpanStyle()
                  .run { copy(fontSize = fontSize * 0.8f, fontStyle = FontStyle.Italic) }) {
                  append(" ($alias)")
                }
              }
            } ?: AnnotatedString(profileInfo.mmid)
          }
          val state = rememberSwipeToViewBoxState()
          val scope = rememberCoroutineScope()

          val trailingContent: @Composable () -> Unit = {
            IconButton({
              state.toggleJob()
            }) {
              Icon(Icons.TwoTone.MoreHoriz, "open menu")
            }
          }
          if (index > 0) {
            HorizontalDivider()
          }
          SwipeToViewBox(state, backgroundContent = {
            Row {
              TextButton(
                onClick = {
                  scope.launch {
                    openDeleteDialog(profileInfo)
                  }
                },
                modifier = Modifier.fillMaxHeight(),
                colors = ButtonDefaults.textButtonColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RectangleShape,
//                enabled = deleteJob == null,
              ) {
                Column(
                  Modifier.padding(horizontal = 8.dp),
                  verticalArrangement = Arrangement.spacedBy(4.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Icon(Icons.TwoTone.DeleteForever, "delete")
                  Text(CommonI18n.delete())
                }
              }
            }
          }) {
            when (profileInfo) {
              is DataController.ProfileBase -> ListItem(
                headlineContent = {
                  Text(
                    DataI18n.uninstalled(),
                    fontStyle = FontStyle.Italic,
                    color = LocalContentColor.current.copy(alpha = 0.8f)
                  )
                },
                supportingContent = {
                  Text(mmidText())
                },
                trailingContent = trailingContent,
              )

              is DataController.ProfileDetail -> ListItem(
                leadingContent = {
                  // TODO MicroModule Icon
                  AppLogo.fromResources(
                    profileInfo.icons,
                    fetchHook = storeNMM.blobFetchHook,
                    base = AppLogo(errorContent = {
                      Icon(Icons.TwoTone.Image, profileInfo.short_name)
                    })
                  ).toIcon().Render(Modifier.size(32.dp))
                },
                modifier = Modifier.clickable { goToDetail(profileInfo) },
                headlineContent = { Text(profileInfo.short_name) },
                supportingContent = { Text(mmidText()) },
                trailingContent = trailingContent,
              )
            }
          }
        }
      }
    }
  }
}