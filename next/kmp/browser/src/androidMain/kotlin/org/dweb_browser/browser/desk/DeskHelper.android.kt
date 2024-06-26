package org.dweb_browser.browser.desk

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.desk.upgrade.NewVersionItem
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.core.std.dns.httpFetch
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod

actual suspend fun DeskNMM.DeskRuntime.startDesktopView(deskSessionId: String) {
  /// 启动对应的Activity视图，如果在后端也需要唤醒到最前面，所以需要在AndroidManifest.xml 配置 launchMode 为 singleTask
  startAppActivity(DesktopActivity::class.java) { intent ->
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
    // 不可以添加 Intent.FLAG_ACTIVITY_NEW_DOCUMENT ，否则 TaskbarActivity 就没发和 DesktopActivity 混合渲染、点击穿透
    intent.putExtras(Bundle().apply {
      putString("deskSessionId", deskSessionId)
    })
  }
}

const val NewVersionUrl =
  "https://source.dwebdapp.com/dweb-browser-apps/dweb-browser/version.json" // 获取最新版本信息

@Serializable
data class LastVersionItem(
  val android: String,
  val version: String,
  val market: Map<String, Version>,
) {
  @Serializable
  data class Version(val version: String)

  fun createNewVersionItem() = NewVersionItem(originUrl = android, versionName = version)
}

actual suspend fun loadApplicationNewVersion(): NewVersionItem? {
  val loadNewVersion = try {
    val response = httpFetch.fetch(
      PureClientRequest(href = NewVersionUrl, method = PureMethod.GET)
    )
    Json.decodeFromString<LastVersionItem>(response.text())
  } catch (e: Exception) {
    debugDesk("NewVersion", "error => ${e.message}")
    null
  }
  return loadNewVersion?.createNewVersionItem()
}

actual fun desktopGridLayout(size: IntSize): GridCells {
  return if (size.width < size.height) {
    GridCells.Fixed(4)
  } else {
    GridCells.Adaptive(80.dp)
  }
}

actual fun desktopTap(): Dp = 0.dp

actual fun desktopBgCircleCount(): Int = 8

actual fun taskBarCloseButtonLineWidth() = 5f

actual fun taskBarCloseButtonUsePopUp() = true

@Composable
@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.DesktopEventDetector(
  onClick: () -> Unit,
  onDoubleClick: () -> Unit,
  onLongClick: () -> Unit
) = this.then(
  combinedClickable(
    indication = null,
    interactionSource = remember { MutableInteractionSource() },
    onClick = onClick,
    onDoubleClick = onDoubleClick,
    onLongClick = onLongClick
  )
)

