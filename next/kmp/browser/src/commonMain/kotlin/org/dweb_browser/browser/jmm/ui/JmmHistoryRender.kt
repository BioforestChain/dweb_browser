package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.AsyncImage
import org.dweb_browser.browser.common.CommonSimpleTopBar
import org.dweb_browser.browser.jmm.JmmHistoryController
import org.dweb_browser.browser.jmm.JmmHistoryMetadata
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.formatDatestamp
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.sys.window.core.WindowRenderScope

@Composable
fun JmmHistoryController.ManagerViewRender(
  modifier: Modifier, windowRenderScope: WindowRenderScope
) {
  val scope = rememberCoroutineScope()
  Column(modifier = with(windowRenderScope) {
    Modifier
      .fillMaxSize()
      .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
      .scale(scale)
  }) {
    CommonSimpleTopBar(BrowserI18nResource.top_bar_title_install()) {
      scope.launch { this@ManagerViewRender.close() }
    }

    if (this@ManagerViewRender.jmmHistoryMetadata.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = BrowserI18nResource.no_apps_data())
      }
      return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      itemsIndexed(this@ManagerViewRender.jmmHistoryMetadata) { _, metadata ->
        JmmViewItem(metadata = metadata)
      }
    }
  }
}

@Composable
fun JmmViewItem(metadata: JmmHistoryMetadata) {
  var showMore by remember { mutableStateOf(false) }
  ListItem(
    headlineContent = {
      Text(
        text = metadata.metadata.name,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.W700
      )
    },
    supportingContent = {
      Row {
        Text(text = metadata.metadata.bundle_size.toSpaceSize())
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = metadata.installTime.formatDatestamp())
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = if (showMore) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
          contentDescription = "More",
          modifier = Modifier.clickableWithNoEffect {
            showMore = !showMore
          }
        )
      }
    },
    leadingContent = {
      Box(modifier = Modifier.height(72.dp), contentAlignment = Alignment.Center) {
        AsyncImage(
          model = metadata.metadata.logo,
          contentDescription = "icon",
          modifier = Modifier.size(56.dp),
          contentScale = ContentScale.Fit
        )
      }
    },
    trailingContent = {
      Box(modifier = Modifier.height(72.dp), contentAlignment = Alignment.Center) {
        Text(
          text = metadata.getName(),
          color = MaterialTheme.colorScheme.background,
          fontWeight = FontWeight.W900,
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { }
        )
      }
    },
  )
  if (showMore) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 72.dp)
    ) {
      TextButton(onClick = { /*TODO*/ }) {
        Text(text = "UnInstall")
      }
    }
  }
}

private fun JmmHistoryMetadata.getName() =
  when (state.state) {
    JmmStatus.Downloading -> "Downloading"
    JmmStatus.Paused -> "Pause"
    JmmStatus.Failed -> "Retry"
    JmmStatus.Init, JmmStatus.Canceled -> "Install"
    JmmStatus.Completed -> "Installing"
    JmmStatus.INSTALLED -> "Open"
    JmmStatus.NewVersion -> "Upgrade"
  }
