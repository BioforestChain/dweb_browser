package org.dweb_browser.browser.jmm.render

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
internal fun TopAppBar(alpha: MutableState<Float>, title: String, onBack: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface.copy(alpha.value))
      //.statusBarsPadding()
      .height(TopBarHeight),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.AutoMirrored.Filled.ArrowBack,
      contentDescription = "Back",
      modifier = Modifier
        .clickable { onBack() }
        .padding(horizontal = HorizontalPadding, vertical = VerticalPadding / 2)
        .size(HeadIconSize)
    )
    Text(
      text = title,
      fontWeight = FontWeight(500),
      fontSize = 18.sp,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha.value)
    )
  }
}