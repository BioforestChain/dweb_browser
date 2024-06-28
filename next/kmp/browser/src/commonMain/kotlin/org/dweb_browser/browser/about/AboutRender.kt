package org.dweb_browser.browser.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.jmm.JsMicroModule

@Composable
fun AboutDetailsItem(modifier: Modifier = Modifier, labelName: String, text: String) {
  Row(
    modifier = modifier.fillMaxWidth().padding(8.dp).height(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = labelName,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurface
    )
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurface,
      softWrap = false,
      overflow = TextOverflow.Ellipsis
    )
  }
}

data class AboutAppInfo(
  val appName: String = "Dweb Browser",
  val appVersion : String,
  val webviewVersion: String,
  val jmmVersion: Int = JsMicroModule.VERSION,
  val jmmPatch: Int = JsMicroModule.PATCH
)

@Composable
fun AboutAppInfoRender(appInfo: AboutAppInfo) {
  Column(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().background(
      color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp)
    )
  ) {
    AboutDetailsItem(labelName = AboutI18nResource.appName.text, text = appInfo.appName)
    AboutDetailsItem(
      labelName = AboutI18nResource.appVersion.text, text = appInfo.appVersion
    )
    AboutDetailsItem(
      labelName = AboutI18nResource.webviewVersion.text, text = appInfo.webviewVersion
    )
    AboutDetailsItem(
      labelName = "JMM ${AboutI18nResource.version.text}", text = appInfo.jmmVersion.toString()
    )
    AboutDetailsItem(
      labelName = "JMM ${AboutI18nResource.patch.text}", text = appInfo.jmmPatch.toString()
    )
  }
}