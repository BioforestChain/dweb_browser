package org.dweb_browser.browser.jmm.render.app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.render.HorizontalPadding
import org.dweb_browser.browser.jmm.render.VerticalPadding
import org.dweb_browser.core.help.types.JmmAppInstallManifest

/**
 * 应用新版本信息部分
 */
@Composable
internal fun NewVersionInfoView(
  jmmAppInstallManifest: JmmAppInstallManifest,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)
      .animateContentSize()
  ) {
    Text(
      text = BrowserI18nResource.JMM.install_info(),
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.size(16.dp))
    Text(
      text = BrowserI18nResource.JMM.install_version() + " ${jmmAppInstallManifest.version}",
      style = MaterialTheme.typography.labelSmall,
    )
    Text(
      text = jmmAppInstallManifest.change_log,
      style = MaterialTheme.typography.bodySmall,
    )
  }
//  Card(onClick = { expanded.value = !expanded.value },
//    modifier = Modifier
//      .animateContentSize().clickable { expanded.value = !expanded.value }) {
//    Column(Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
//      Text(
//        text = BrowserI18nResource.JMM.install_update_log(),
//        style = MaterialTheme.typography.labelMedium,
//        modifier = Modifier.padding(vertical = 6.dp)
//      )
//      // TODO Markdown Support
//      Text(
//        text = jmmAppInstallManifest.change_log,
//        maxLines = if (expanded.value) Int.MAX_VALUE else 2,
//        overflow = TextOverflow.Ellipsis,
//        style = MaterialTheme.typography.bodyMedium,
//      )
//    }
//  }
}