package org.dweb_browser.browser.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommonSimpleTopBar(title: String, onBack: () -> Unit) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    IconButton(onClick = { onBack() }) {
      Icon(
        imageVector = Icons.Default.ArrowBackIosNew,
        contentDescription = "Back",
        modifier = Modifier
          .size(48.dp)
          .padding(8.dp)
      )
    }
    Text(
      text = title,
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 48.dp, end = 48.dp),
      textAlign = TextAlign.Center,
      fontSize = 18.sp,
      fontWeight = FontWeight.W700
    )
  }
}