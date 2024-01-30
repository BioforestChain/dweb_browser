package org.dweb_browser.browser.jmm.render.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = HorizontalPadding, vertical = VerticalPadding),
    verticalAlignment = Alignment.CenterVertically
  ) {
    CoilAsyncImage(
      model = jmmAppInstallManifest.logo,
      contentDescription = "AppIcon",
      modifier = Modifier
        .size(size)
        .clip(RoundedCornerShape(16.dp))
        .background(MaterialTheme.colorScheme.surface)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .height(size)
    ) {
      Text(
        text = jmmAppInstallManifest.name,
        maxLines = 2,
        fontWeight = FontWeight(500),
        fontSize = 22.sp,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = jmmAppInstallManifest.short_name,
        maxLines = 1,
        color = MaterialTheme.colorScheme.outlineVariant,
        overflow = TextOverflow.Ellipsis,
        fontSize = 12.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = BrowserI18nResource.jmm_install_version() + " ${jmmAppInstallManifest.version}",
        fontSize = 12.sp
      )
      /*Text(
        text = buildAnnotatedString {
          append("人工复检 · ")
          withStyle(style = SpanStyle(color = Color.Green)) { append("无广告") }
        }, fontSize = 12.sp
      )*/
    }
  }
}