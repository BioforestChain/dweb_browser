package org.dweb_browser.browser.jmm.render.app

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
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
 * 应用介绍描述部分
 */
@Composable
internal fun AppIntroductionView(jmmAppInstallManifest: JmmAppInstallManifest) {
  Column(modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(
      text = BrowserI18nResource.JMM.install_introduction(),
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.size(16.dp))
    Box(modifier = Modifier.animateContentSize()) {
      Text(
        text = jmmAppInstallManifest.description ?: "",
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}