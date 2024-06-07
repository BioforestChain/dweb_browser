package org.dweb_browser.browser.jmm.render.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.LocalJmmInstallerController
import org.dweb_browser.browser.jmm.render.CustomerDivider
import org.dweb_browser.browser.jmm.render.HorizontalPadding
import org.dweb_browser.browser.jmm.render.VerticalPadding
import org.dweb_browser.browser.jmm.render.toContent
import org.dweb_browser.core.CoreI18nResource
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.encodeURIComponent
import org.dweb_browser.helper.toSpaceSize

/**
 * 应用的其他相关内容
 */
@Composable
internal fun OtherInfoView(jmmAppInstallManifest: JmmAppInstallManifest) {
  Column(modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(
      text = BrowserI18nResource.JMM.install_info(),
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(HorizontalPadding))
    OtherItemView(
      type = BrowserI18nResource.JMM.install_info_dev(),
      content = jmmAppInstallManifest.author.toContent()
    )
    jmmAppInstallManifest.homepage_url?.also { homepage_url ->
      val controller = LocalJmmInstallerController.current
      OtherItemView(
        modifier = Modifier.clickable {
          controller.jmmNMM.scopeLaunch(cancelable = true) {
            controller.jmmNMM.nativeFetch("dweb://openinbrowser?url=${homepage_url.encodeURIComponent()}")
            controller.closeSelf()
          }
        },
        type = BrowserI18nResource.JMM.install_info_homepage(),
        content = homepage_url,
        contentTextStyle = TextStyle(color = MaterialTheme.colorScheme.primary)
      )
    }
    OtherItemView(
      type = BrowserI18nResource.JMM.install_info_size(),
      content = jmmAppInstallManifest.bundle_size.toSpaceSize()
    )
    OtherItemView(
      type = BrowserI18nResource.JMM.install_info_type(),
      content = jmmAppInstallManifest.categories.print()
    )
    OtherItemView(
      type = BrowserI18nResource.JMM.install_info_copyright(),
      content = jmmAppInstallManifest.author.firstOrNull() ?: jmmAppInstallManifest.name
    )
  }
}

/**
 * @param largeContent 该字段如果有数据，表示允许展开，查看详细信息
 */
@Composable
private fun OtherItemView(
  modifier: Modifier = Modifier,
  type: String,
  content: String,
  contentModifier: Modifier = Modifier,
  contentTextStyle: TextStyle? = null,
  largeContent: String? = null,
  onClick: (() -> Unit)? = null,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(text = type, color = MaterialTheme.colorScheme.outline)

      Text(
        text = content,
        modifier = contentModifier.weight(1f),
        style = TextStyle(
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.End,
        ).merge(contentTextStyle),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
    CustomerDivider()
  }
}

fun List<MICRO_MODULE_CATEGORY>.print(): String {
  return joinToString(", ") { category ->
    CoreI18nResource.Category.ALL[category]?.res?.text ?: category.name
  }
}