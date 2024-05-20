package org.dweb_browser.browser.jmm.render.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.jmm.render.HeadHeight
import org.dweb_browser.browser.jmm.render.HorizontalPadding
import org.dweb_browser.browser.jmm.render.VerticalPadding
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.pure.image.compose.CoilAsyncImage

/**
 * 顶部的头像和应用名称
 */
@Composable
internal fun AppInfoHeadView(jmmAppInstallManifest: JmmAppInstallManifest) {
  val size = HeadHeight - VerticalPadding * 2
  Column(
    modifier = Modifier.fillMaxWidth()
      .padding(horizontal = HorizontalPadding, vertical = VerticalPadding),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().height(size / 2),
      verticalAlignment = Alignment.CenterVertically
    ) {
      CoilAsyncImage(
        model = jmmAppInstallManifest.logo,
        contentDescription = "AppIcon",
        modifier = Modifier.size(size / 2).clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
      )
      Spacer(modifier = Modifier.width(16.dp))
      Text(
        text = jmmAppInstallManifest.name,
        maxLines = 2,
        fontWeight = FontWeight(500),
        fontSize = 22.sp,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    Spacer(modifier = Modifier.width(16.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 应用id和是否校验通过
      Row(
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // TODO: 验证成功失败，使用不同的图标
        Icon(
          imageVector = Icons.Outlined.Verified, // Icons.Outlined.Dangerous
          contentDescription = BrowserI18nResource.IconDescription.verified.text,
          modifier = Modifier.size(16.dp),
          tint = Color.Blue
        )

        Text(
          text = jmmAppInstallManifest.id,
          maxLines = 1,
          color = MaterialTheme.colorScheme.onBackground,
          overflow = TextOverflow.Ellipsis,
          fontSize = 12.sp,
          style = TextStyle.Default.copy(
            lineHeightStyle = LineHeightStyle(
              alignment = LineHeightStyle.Alignment.Center,
              trim = LineHeightStyle.Trim.None,
            )
          )
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      Text(
        text = BrowserI18nResource.jmm_install_version() + " ${jmmAppInstallManifest.version}",
        fontSize = 12.sp,
        style = TextStyle.Default.copy(
          lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
          )
        )
      )/*Text(
        text = buildAnnotatedString {
          append("人工复检 · ")
          withStyle(style = SpanStyle(color = Color.Green)) { append("无广告") }
        }, fontSize = 12.sp
      )*/
    }
  }
}