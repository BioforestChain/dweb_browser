package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.CommonSimpleTopBar
import org.dweb_browser.browser.jmm.JmmHistoryController
import org.dweb_browser.core.help.types.JmmAppInstallManifest
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

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      itemsIndexed(jmmMetadataList) { _, metadata ->
        JmmViewItem(metadata = metadata)
        Spacer(
          modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
        )
      }
    }
  }
}

@Composable
fun JmmViewItem(metadata: JmmAppInstallManifest) {
  ListItem(
    headlineContent = {
      Text(text = metadata.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    },
    supportingContent = {
      Text(
        text = metadata.description ?: "no description",
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )
    },
    leadingContent = {
      AsyncImage(
        model = metadata.logo,
        contentDescription = "icon",
        modifier = Modifier.size(56.dp),
        contentScale = ContentScale.Fit
      )
    },
    trailingContent = {
      Button(onClick = { /*TODO*/ }) {
        Text(text = "Open")
      }
    }
  )
}