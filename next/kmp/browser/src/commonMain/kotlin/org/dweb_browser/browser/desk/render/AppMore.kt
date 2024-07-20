package org.dweb_browser.browser.desk.render

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.desk.model.AppMenuModel
import org.dweb_browser.browser.desk.model.AppMenuType

@Composable
internal fun AppMoreRender(
  displays: List<AppMenuModel>,
  modifier: Modifier,
  action: (AppMenuType) -> Unit,
) {
  Row(
    modifier = modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.5f)),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    displays.forEach {
      Column(
        modifier = Modifier.padding(4.dp).fillMaxSize().weight(1f).clickable(enabled = it.enable) {
          action(it.type)
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        Icon(
          imageVector = it.type.data.icon,
          contentDescription = null,
          tint = if (it.enable) Color.Black else Color.Gray
        )
        Text(
          it.type.data.title,
          Modifier,
          fontSize = 12.sp,
          color = if (it.enable) Color.Black else Color.Gray
        )
      }
    }
  }
}