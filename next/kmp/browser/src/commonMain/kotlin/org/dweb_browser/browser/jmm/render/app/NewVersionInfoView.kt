package org.dweb_browser.browser.jmm.render.app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.jmm.render.HorizontalPadding
import org.dweb_browser.browser.jmm.render.VerticalPadding
import org.dweb_browser.core.help.types.JmmAppInstallManifest

/**
 * 应用新版本信息部分
 */
@Composable
internal fun NewVersionInfoView(jmmAppInstallManifest: JmmAppInstallManifest) {
  val expanded = remember { mutableStateOf(false) }
  Column(modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(
      text = "更新日志",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = "版本 ${jmmAppInstallManifest.version}",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.outline,
      modifier = Modifier.padding(vertical = 6.dp)
    )

    Box(modifier = Modifier
      .animateContentSize()
      .clickable { expanded.value = !expanded.value }) {
      Text(
        text = jmmAppInstallManifest.change_log,
        maxLines = if (expanded.value) Int.MAX_VALUE else 2,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}