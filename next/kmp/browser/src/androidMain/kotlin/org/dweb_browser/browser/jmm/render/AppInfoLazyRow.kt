package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.microservice.help.types.JmmAppInstallManifest

/**
 * 中间的横向数据
 */
@Composable
internal fun AppInfoLazyRow(jmmAppInstallManifest: JmmAppInstallManifest) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .height(AppInfoHeight),
    contentPadding = PaddingValues(HorizontalPadding),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    item { // 评分
      DoubleRowItem(first = "4.9 分", second = "999+ 评论")
    }
    item { // 安装次数
      DoubleRowItem(first = "9527 万", second = "次安装")
    }
    item { // 年龄限制
      DoubleRowItem(first = "18+", second = "年满 18 周岁")
    }
    item { // 大小
      DoubleRowItem(first = jmmAppInstallManifest.bundle_size.toSpaceSize(), second = "大小")
    }
  }
}

@Composable
internal fun DoubleRowItem(first: String, second: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = first,
      fontSize = 18.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.outline,
      maxLines = 1
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = second,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.outlineVariant,
      maxLines = 1
    )
  }
}
