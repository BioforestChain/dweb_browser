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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.jmm.JmmMetadata
import org.dweb_browser.browser.jmm.JmmRenderController
import org.dweb_browser.browser.jmm.JmmTabs

@Composable
fun JmmRenderController.JmmListView(
  modifier: Modifier,
  curTab: JmmTabs = JmmTabs.Installed,
  onTabClick: (JmmTabs) -> Unit,
  onOpenDetail: (JmmMetadata) -> Unit
) {
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
            onTabClick(jmmTab)
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
        var list by remember(jmmTab, all) { mutableStateOf(jmmTab.listFilter(all.values)) }
        /// 状态变更之后，进行计算，触发重组，重新生成list展示
        val jmmStatusList = derivedStateOf { list.map { it.state.state } }

        LaunchedEffect(jmmStatusList) {
          list = jmmTab.listFilter(all.values)
        }

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
                onOpenDetail = { onOpenDetail(metadata) },
              )
            }
          }
        }
      }
    }
  }
}

