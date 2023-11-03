package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.dweb_browser.browser.jmm.JmmInstallerController
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.sys.window.core.WindowRenderScope

@Composable
fun JmmInstallerController.ManagerViewRender(
  modifier: Modifier, windowRenderScope: WindowRenderScope
) {
  Box(modifier = with(windowRenderScope) {
    Modifier
      .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
      .scale(scale)
  }) {

    LazyColumn(modifier = Modifier.fillMaxSize()) {
      itemsIndexed(jmmMetadataList) { index, metadata ->
        JmmViewItem(metadata = metadata)
      }
    }
  }
}

@Composable
fun JmmViewItem(metadata: JmmAppInstallManifest) {
  ListItem(
    headlineContent = {
      Text(text = metadata.name)
    },
    supportingContent = {
      Text(text = metadata.description ?: "no description")
    },
    leadingContent = {
      AsyncImage(model = metadata.logo, contentDescription = "icon")
    },
    trailingContent = {
      Image(imageVector = Icons.Default.OpenInBrowser, contentDescription = "Open")
    }
  )
}