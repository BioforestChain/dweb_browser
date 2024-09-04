package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.jmm.JmmRenderController
import org.dweb_browser.browser.jmm.JmmTabs

@Composable
fun JmmRenderController.JmmListView(modifier: Modifier, showDetailButton: Boolean = true) {

  var curTab by remember { mutableStateOf(JmmTabs.Installed) }
  Column(
    modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    SingleChoiceSegmentedButtonRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
      JmmTabs.entries.forEachIndexed { index, jmmTab ->
        val selected = curTab == jmmTab
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index = index, count = JmmTabs.entries.size),
          onClick = {
            curTab = jmmTab
          },
          selected = selected,
          icon = { Icon(imageVector = jmmTab.vector, contentDescription = jmmTab.title()) },
          label = { Text(text = jmmTab.title()) },
        )
      }
    }

    val all by historyMetadataMap()
    for (jmmTab in JmmTabs.entries) {
      if (curTab == jmmTab) {
        // TODO 这个后续需要优化，目前下载完成后，历史展示没有直接刷新
        val list = remember(jmmTab, all) { jmmTab.listFilter(all.values) }

        when (list.size) {
          0 -> Box(
            Modifier.fillMaxSize(), contentAlignment = Alignment.Center
          ) {
            Text("暂无数据")
          }

          else -> LazyColumn {
            items(list, key = { it.manifest.id }) { metadata ->
              JmmListItem(
                jmmMetadata = metadata,
                onRemove = { removeHistoryMetadata(metadata) },
                onUnInstall = { unInstall(metadata) },
                onOpenDetail = { openDetail(metadata) },
              )
            }
          }
        }
      }
    }
  }
}

