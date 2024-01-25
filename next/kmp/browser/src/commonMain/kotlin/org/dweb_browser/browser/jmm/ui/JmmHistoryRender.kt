package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.common.AsyncImage
import org.dweb_browser.browser.common.CommonSimpleTopBar
import org.dweb_browser.browser.common.SegmentedButton
import org.dweb_browser.browser.common.SingleChoiceSegmentedButtonRow
import org.dweb_browser.browser.jmm.JmmHistoryController
import org.dweb_browser.browser.jmm.JmmHistoryMetadata
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.browser.jmm.JmmTabs
import org.dweb_browser.helper.compose.NoDataRender
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.formatDatestampByMilliseconds
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.sys.window.core.WindowRenderScope
import org.dweb_browser.sys.window.render.LocalWindowController

@Composable
fun JmmHistoryController.ManagerViewRender(
  modifier: Modifier, windowRenderScope: WindowRenderScope
) {
  val scope = rememberCoroutineScope()
  var curTab by remember { mutableStateOf(JmmTabs.NoInstall) }
  val win = LocalWindowController.current
  win.GoBackHandler {
    win.hide()
  }

  Column(modifier = windowRenderScope.run {
    modifier
      .fillMaxSize()
      .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
      .scale(scale)
  }) {
    CommonSimpleTopBar(BrowserI18nResource.top_bar_title_install()) {
      scope.launch { this@ManagerViewRender.close() }
    }

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      JmmTabs.entries.forEachIndexed { index, jmmTab ->
        SegmentedButton(
          selected = index == curTab.index,
          onClick = { curTab = JmmTabs.entries[index] },
          shape = RoundedCornerShape(16.dp),
          icon = { Icon(imageVector = jmmTab.vector, contentDescription = jmmTab.title()) },
          label = { Text(text = jmmTab.title()) }
        )
      }
    }

    JmmTabsView(curTab)
  }
}

@Composable
fun JmmHistoryController.JmmTabsView(tab: JmmTabs) {
  val scope = rememberCoroutineScope()
  // 这个后续需要优化，目前下载完成后，历史展示没有直接刷新
  val list = jmmHistoryMetadata.filter {
    (tab == JmmTabs.Installed && it.state.state == JmmStatus.INSTALLED) ||
        (tab == JmmTabs.NoInstall && it.state.state != JmmStatus.INSTALLED)
  }

  if (list.isEmpty()) {
    NoDataRender(BrowserI18nResource.no_apps_data())
    return
  }

  LazyColumn(modifier = Modifier.fillMaxSize()) {
    itemsIndexed(list) { _, metadata ->
      JmmViewItem(
        jmmHistoryMetadata = metadata,
        buttonClick = { scope.launch { this@JmmTabsView.buttonClick(metadata) } },
        uninstall = { scope.launch { this@JmmTabsView.unInstall(metadata) } },
        detail = { scope.launch { this@JmmTabsView.openInstallerView(metadata) } }
      )
    }
  }
}

@Composable
fun JmmViewItem(
  jmmHistoryMetadata: JmmHistoryMetadata,
  buttonClick: () -> Unit,
  uninstall: () -> Unit,
  detail: () -> Unit
) {
  var showMore by remember(jmmHistoryMetadata) { mutableStateOf(false) }

  ListItem(
    headlineContent = {
      Text(
        text = jmmHistoryMetadata.metadata.name,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.W700
      )
    },
    supportingContent = {
      Row {
        Text(text = jmmHistoryMetadata.metadata.bundle_size.toSpaceSize())
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = jmmHistoryMetadata.installTime.formatDatestampByMilliseconds())
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = if (showMore) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
          contentDescription = "More",
        )
      }
    },
    leadingContent = {
      Box(modifier = Modifier.height(72.dp), contentAlignment = Alignment.Center) {
        key(jmmHistoryMetadata.metadata.logo) {
          AsyncImage(
            model = jmmHistoryMetadata.metadata.logo,
            contentDescription = "icon",
            modifier = Modifier.size(56.dp),
          )
        }
      }
    },
    trailingContent = {
      Box(
        modifier = Modifier.size(64.dp).clickableWithNoEffect { buttonClick() },
        contentAlignment = Alignment.Center
      ) {
        val progress = with(jmmHistoryMetadata.state) {
          if (total > 0) current * 1.0f / total else 0f
        }
        val primary = MaterialTheme.colorScheme.primary
        when (jmmHistoryMetadata.state.state) {
          JmmStatus.Downloading -> {
            Box(
              modifier = Modifier.size(40.dp),
              contentAlignment = Alignment.Center
            ) {
              // 画圆
              Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                  color = primary,
                  startAngle = -90f,
                  sweepAngle = progress * 360f,
                  useCenter = false,
                  style = Stroke(width = 8f)
                )
              }
              // 画图标
              Image(
                imageVector = Icons.Default.Download,
                contentDescription = "Download",
                modifier = Modifier.clip(CircleShape).size(36.dp),
                contentScale = ContentScale.FillBounds
              )
            }
          }

          JmmStatus.Paused -> {
            Box(
              modifier = Modifier
                .size(width = 64.dp, height = 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
            ) {
              Box(
                modifier = Modifier
                  .size(width = (64 * progress).dp, height = 30.dp)
                  .background(MaterialTheme.colorScheme.primary)
              )

              Text(
                text = jmmHistoryMetadata.state.state.showText(),
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.W900,
                modifier = Modifier.align(Alignment.Center),
              )
            }
          }

          else -> {
            Box(
              modifier = Modifier
                .size(width = 64.dp, height = 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = jmmHistoryMetadata.state.state.showText(),
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.W900,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      }
    },
    modifier = Modifier.clickableWithNoEffect {
      showMore = !showMore
    }
  )
  if (showMore) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 72.dp)
    ) {
      if (jmmHistoryMetadata.state.state == JmmStatus.INSTALLED) {
        TextButton(onClick = uninstall) {
          Text(text = BrowserI18nResource.jmm_history_uninstall())
        }
      }

      TextButton(onClick = detail) {
        Text(text = BrowserI18nResource.jmm_history_details())
      }
    }
  }
}

@Composable
private fun JmmStatus.showText() =
  when (this) {
    JmmStatus.Downloading -> BrowserI18nResource.install_button_downloading()
    JmmStatus.Paused -> BrowserI18nResource.install_button_paused()
    JmmStatus.Failed -> BrowserI18nResource.install_button_retry2()
    JmmStatus.Init, JmmStatus.Canceled -> BrowserI18nResource.install_button_install()
    JmmStatus.Completed -> BrowserI18nResource.install_button_installing()
    JmmStatus.INSTALLED -> BrowserI18nResource.install_button_open()
    JmmStatus.NewVersion -> BrowserI18nResource.install_button_update()
  }