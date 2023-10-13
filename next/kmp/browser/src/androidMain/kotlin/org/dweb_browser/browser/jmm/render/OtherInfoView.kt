package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.dweb_browser.core.help.types.JmmAppInstallManifest
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.helper.toSpaceSize

/**
 * 应用的其他相关内容
 */
@Composable
internal fun OtherInfoView(jmmAppInstallManifest: JmmAppInstallManifest) {
  Column(modifier = Modifier.padding(horizontal = HorizontalPadding, vertical = VerticalPadding)) {
    Text(
      text = "信息",
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(HorizontalPadding))
    OtherItemView(type = "开发者", content = jmmAppInstallManifest.author?.toContent() ?: "me")
    OtherItemView(type = "大小", content = jmmAppInstallManifest.bundle_size.toSpaceSize())
    OtherItemView(type = "类别", content = jmmAppInstallManifest.categories.print())
    OtherItemView(type = "语言", content = "中文")
    OtherItemView(type = "年龄分级", content = "18+")
    OtherItemView(
      type = "版权",
      content = "@${jmmAppInstallManifest.author?.firstOrNull() ?: jmmAppInstallManifest.name}"
    )
  }
}

fun List<MICRO_MODULE_CATEGORY>.print(): String {
  val result = StringBuffer()
  this.forEach { category ->
    result.append(category.name)
  }
  return result.toString()
}