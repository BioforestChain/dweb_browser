package org.dweb_browser.browser.jmm.render.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.JmmI18nResource
import org.dweb_browser.browser.jmm.LocalJmmDetailController
import org.dweb_browser.browser.jmm.render.HorizontalPadding
import org.dweb_browser.browser.jmm.render.VerticalPadding
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
internal fun OtherInfoView(
  jmmAppInstallManifest: JmmAppInstallManifest,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(
      text = JmmI18nResource.tab_param(),
      style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.size(16.dp))
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Label(JmmI18nResource.install_mmid())

      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // TODO: 验证成功失败，使用不同的图标
        Icon(
          imageVector = Icons.Outlined.Verified, // Icons.Outlined.Dangerous
          contentDescription = BrowserI18nResource.IconDescription.verified.text,
          modifier = Modifier.size(14.dp),
          tint = Color.Blue
        )
        Spacer(modifier = Modifier.width(8.dp))
        Info(jmmAppInstallManifest.id)
      }
    }
    HorizontalDivider()
    OtherItemView(
      type = JmmI18nResource.install_version(), content = jmmAppInstallManifest.version
    )
    HorizontalDivider()
    OtherItemView(
      type = JmmI18nResource.install_info_dev(),
      content = jmmAppInstallManifest.author.joinToString(", ")
    )
    HorizontalDivider()
    jmmAppInstallManifest.homepage_url?.also { homepage_url ->
      val controller = LocalJmmDetailController.current
      OtherItemView(
        modifier = Modifier.clickable {
          controller.jmmNMM.scopeLaunch(cancelable = true) {
            controller.jmmNMM.nativeFetch("dweb://openinbrowser?url=${homepage_url.encodeURIComponent()}")
            controller.closeBottomSheet()
          }
        },
        type = JmmI18nResource.install_info_homepage(),
        content = homepage_url,
        contentTextStyle = TextStyle(color = MaterialTheme.colorScheme.primary)
      )
    }
    HorizontalDivider()
    OtherItemView(
      type = JmmI18nResource.install_info_size(),
      content = jmmAppInstallManifest.bundle_size.toSpaceSize()
    )
    HorizontalDivider()
    OtherItemView(
      type = JmmI18nResource.install_info_type(),
      content = jmmAppInstallManifest.categories.print()
    )
    HorizontalDivider()
    OtherItemView(
      type = JmmI18nResource.install_info_copyright(),
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
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Label(type)
    Info(content, contentModifier, contentTextStyle)
  }
}

@Composable
private fun RowScope.Label(
  text: String,
  modifier: Modifier = Modifier,
  textStyle: TextStyle? = null,
) = Text(
  text = text, color = MaterialTheme.colorScheme.outline,
  modifier = modifier.weight(1f, false),
  style = MaterialTheme.typography.labelSmall.merge(textStyle),
)

@Composable
private fun RowScope.Info(
  content: String,
  modifier: Modifier = Modifier,
  textStyle: TextStyle? = null,
) = Text(
  text = content,
  modifier = modifier.weight(1f, false),
  color = MaterialTheme.colorScheme.onSurface,
  textAlign = TextAlign.End,
  style = MaterialTheme.typography.bodyMedium.merge(textStyle),
  maxLines = 1,
  overflow = TextOverflow.Ellipsis,
)

fun List<MICRO_MODULE_CATEGORY>.print(): String {
  return joinToString(", ") { category ->
    CoreI18nResource.Category.ALL[category]?.res?.text ?: category.name
  }
}