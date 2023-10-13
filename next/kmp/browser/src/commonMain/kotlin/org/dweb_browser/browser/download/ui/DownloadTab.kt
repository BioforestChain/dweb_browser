package org.dweb_browser.browser.download.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.datetime.LocalDate
import org.dweb_browser.browser.download.DownloadTask

enum class DownloadTabName(val title: String, val index: Int) {
  Downloads("Downloads", 0), Files("Files", 1)
  ;
}

val LocalTabRowState = compositionLocalOf {
  mutableStateOf(DownloadTabName.Downloads)
}

@Composable
fun DownloadTab() {
  val state = LocalTabRowState.current

  Column {
    TabRow(selectedTabIndex = state.value.index) {
      DownloadTabName.entries.forEach { tab ->
        Tab(
          selected = state.value.index == tab.index,
          onClick = { state.value = DownloadTabName.values()[tab.index] }
        ) {
          Text(text = tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }

    DownloadTabItem()
  }
}

@Composable
fun DownloadTabItem() {
  val state = LocalTabRowState.current
  val list = remember { mutableStateListOf<DownloadTask>() }
  LaunchedEffect(state) {

  }

}