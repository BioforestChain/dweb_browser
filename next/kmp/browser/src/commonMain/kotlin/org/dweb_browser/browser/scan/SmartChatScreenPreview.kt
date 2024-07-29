package org.dweb_browser.browser.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.dweb_browser.browser.BrowserI18nResource

@Composable
fun SmartScanController.ChatScreenPreview(
  messages: List<BarcodeResult>
) {
  Box(
    modifier = Modifier.fillMaxSize().padding(bottom = 50.dp),
    contentAlignment = Alignment.BottomCenter
  ) {
    Column(
      Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.Bottom,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      filterChats(messages).forEach { message ->
        key(message.data) {
          CustomSnackBar(message.data) {
            onSuccess(message.data)
          }
        }
      }
    }
  }
}

@Composable
fun CustomSnackBar(message: String, onDismiss: () -> Unit) {
  Box(
    modifier = Modifier
      .padding(6.dp)
      .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp)) // 透明背景
      .padding(horizontal = 6.dp, vertical = 0.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = message,
        color = Color.White,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable { onDismiss() }
      )
      TextButton(onClick = onDismiss) {
        Text(
          BrowserI18nResource.QRCode.Action.text,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}