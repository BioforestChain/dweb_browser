package org.dweb_browser.browser.jmm.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.twotone.BrokenImage
import androidx.compose.material.icons.twotone.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmMetadata
import org.dweb_browser.browser.jmm.JmmRenderController
import org.dweb_browser.browser.jmm.JmmStatus
import org.dweb_browser.browser.jmm.JmmTabs
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.helper.compose.LazySwipeColumn
import org.dweb_browser.helper.compose.ListDetailPaneScaffold
import org.dweb_browser.helper.compose.ListDetailPaneScaffoldRole
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.produceEvent
import org.dweb_browser.helper.compose.rememberListDetailPaneScaffoldNavigator
import org.dweb_browser.helper.formatDatestampByMilliseconds
import org.dweb_browser.helper.platform.theme.dimens
import org.dweb_browser.helper.toSpaceSize
import org.dweb_browser.pure.image.compose.PureImageLoader
import org.dweb_browser.pure.image.compose.SmartLoad
import org.dweb_browser.sys.window.core.WindowContentRenderScope
import org.dweb_browser.sys.window.core.WindowContentScaffoldWithTitleText
import org.dweb_browser.sys.window.render.LocalWindowController


@Composable
expect fun JmmRenderController.Render(
  modifier: Modifier,
  windowRenderScope: WindowContentRenderScope,
)

@Composable
internal fun JmmRenderController.CommonRender(
  modifier: Modifier, windowRenderScope: WindowContentRenderScope,
) {
  val navigator = rememberListDetailPaneScaffoldNavigator()
  val win = LocalWindowController.current
  win.navigation.GoBackHandler(enabled = navigator.canNavigateBack()) {
    navigator.navigateBack()
    closeDetail()
  }
  LaunchedEffect(detailController) {
    if (detailController != null) {
      navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
    }
  }

  ListDetailPaneScaffold(
    listPane = {
      BoxWithConstraints {
        windowRenderScope.copyFrom(this).WindowContentScaffoldWithTitleText(
          modifier = modifier,
          topBarTitleText = BrowserI18nResource.top_bar_title_install(),
        ) { innerPadding ->
          JmmListView(Modifier.padding(innerPadding))
        }
      }
    },
    detailPane = {
      when (val detail = detailController) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text("未选择要展示的详情")
        }

        else -> BoxWithConstraints {
          detail.Render(
            Modifier.fillMaxSize(), windowRenderScope.copyFrom(this)
          )
        }
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JmmRenderController.JmmListView(modifier: Modifier, showDetailButton: Boolean = true) {

  var curTab by remember { mutableStateOf(JmmTabs.NoInstall) }
  Column(
    modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    SingleChoiceSegmentedButtonRow(
      modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.dimens.medium)
    ) {
      JmmTabs.entries.forEachIndexed { index, jmmTab ->
        SegmentedButton(shape = SegmentedButtonDefaults.itemShape(
          index = index, count = JmmTabs.entries.size
        ),
          onClick = { curTab = JmmTabs.entries[index] },
          selected = index == curTab.index,
          icon = { Icon(imageVector = jmmTab.vector, contentDescription = jmmTab.title()) },
          label = { Text(text = jmmTab.title()) })
      }
    }

    // 这个后续需要优化，目前下载完成后，历史展示没有直接刷新
    val list = getHistoryMetadataMap().values.filter {
      when (curTab) {
        JmmTabs.Installed -> it.state.state == JmmStatus.INSTALLED
        JmmTabs.NoInstall -> it.state.state != JmmStatus.INSTALLED
      }
    }.sortedByDescending { it.upgradeTime }

    LazySwipeColumn(items = list,
      key = { item -> item.manifest.id },
      onRemove = { item -> removeHistoryMetadata(item) },
      noDataValue = BrowserI18nResource.no_apps_data(),
      background = {
        Box(
          Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        )
      }) { metadata ->
      JmmViewItem(
        jmmMetadata = metadata,
        buttonClick = produceEvent(metadata, scope = jmmNMM.getRuntimeScope()) {
          buttonClick(metadata)
        },
        uninstall = { unInstall(metadata) },
        showDetailButton = showDetailButton,
        detail = { openDetail(metadata) },
      )
    }
  }
}

@Composable
fun JmmAppInstallManifest.IconRender(size: Dp = 36.dp) {
  key(logo) {
    val modifier = remember(size) { Modifier.size(size).clip(RoundedCornerShape(size / 5)) }
    PureImageLoader.SmartLoad(logo, size, size).with(onBusy = {
      Image(
        imageVector = Icons.TwoTone.Image,
        contentDescription = it,
        modifier = modifier,
        alpha = 0.5f
      )
    }, onError = {
      Image(
        imageVector = Icons.TwoTone.BrokenImage,
        contentDescription = "icon load error",
        modifier = modifier,
      )
    }) {
      Image(
        bitmap = it,
        contentDescription = "${short_name}'s icon",
        modifier = modifier,
      )
    }
  }
}

@Composable
fun JmmViewItem(
  jmmMetadata: JmmMetadata,
  buttonClick: () -> Unit,
  uninstall: () -> Unit,
  showDetailButton: Boolean,
  detail: () -> Unit,
) {
  var showMore by remember(jmmMetadata) { mutableStateOf(false) }

  Column {
    ListItem(headlineContent = {
      Text(
        text = jmmMetadata.manifest.name,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.W700
      )
    }, supportingContent = {
      Column {
        Text(
          text = jmmMetadata.manifest.version, fontWeight = FontWeight.SemiBold
        )
        Row(
          modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(text = jmmMetadata.manifest.bundle_size.toSpaceSize())
          Text(text = jmmMetadata.installTime.formatDatestampByMilliseconds())
        }
      }
    }, leadingContent = {
      Box(modifier = Modifier.height(72.dp), contentAlignment = Alignment.Center) {
        jmmMetadata.manifest.IconRender()
      }
    }, trailingContent = {
      Box(
        modifier = Modifier.size(64.dp).clickableWithNoEffect { buttonClick() },
        contentAlignment = Alignment.Center
      ) {
        val progress = jmmMetadata.state.progress
        val primary = MaterialTheme.colorScheme.primary
        when (jmmMetadata.state.state) {
          JmmStatus.Downloading, JmmStatus.Paused -> {
            Box(
              modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center
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
              val isDownloading = jmmMetadata.state.state == JmmStatus.Downloading
              Image(
                imageVector = if (isDownloading) Icons.Default.Download else Icons.Default.Pause,
                contentDescription = if (isDownloading) "Download" else "Pause",
                modifier = Modifier.size(38.dp).clip(CircleShape)
                  .background(MaterialTheme.colorScheme.outlineVariant).padding(2.dp),
                contentScale = ContentScale.FillBounds
              )
            }
          }

          else -> {
            Box(
              modifier = Modifier.size(width = 64.dp, height = 30.dp).clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center
            ) {
              Text(
                text = jmmMetadata.state.state.showText(),
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.W900,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      }
    }, modifier = Modifier.clickableWithNoEffect {
      showMore = !showMore;
      if (!showDetailButton) {
        detail()
      }
    })
    if (showMore) {
      val rowItems = mutableListOf<@Composable RowScope.() -> Unit>()
      if (jmmMetadata.state.state == JmmStatus.INSTALLED) {
        rowItems += {
          TextButton(onClick = uninstall) {
            Text(text = BrowserI18nResource.JMM.history_uninstall())
          }
        }
      }
      if (showDetailButton) {
        rowItems += {
          TextButton(onClick = detail) {
            Text(text = BrowserI18nResource.JMM.history_details())
          }
        }
      }
      if (rowItems.isNotEmpty()) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 72.dp)) {
          rowItems.forEach { it() }
        }
      }
    }
  }
}

@Composable
private fun JmmStatus.showText() = when (this) {
  JmmStatus.Downloading -> BrowserI18nResource.install_button_downloading()
  JmmStatus.Paused -> BrowserI18nResource.install_button_paused()
  JmmStatus.Failed -> BrowserI18nResource.install_button_retry2()
  JmmStatus.Init, JmmStatus.Canceled -> BrowserI18nResource.install_button_install()
  JmmStatus.Completed -> BrowserI18nResource.install_button_installing()
  JmmStatus.INSTALLED -> BrowserI18nResource.install_button_open()
  JmmStatus.NewVersion -> BrowserI18nResource.install_button_update()
  JmmStatus.VersionLow -> BrowserI18nResource.install_button_open() // 理论上历史列表应该不存在这状态
}