package org.dweb_browser.browser.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.sys.toast.ext.showToast
import org.dweb_browser.sys.window.core.constant.LocalWindowMM

@Composable
internal fun SmartScanController.ChatScreenPreview(
  listState: LazyListState, messages: List<BarcodeResultDrawer>,
) {
  Box(
    modifier = Modifier.fillMaxSize().padding(bottom = 72.dp, start = 8.dp, end = 8.dp),
    contentAlignment = Alignment.BottomCenter
  ) {
    val density = LocalDensity.current.density

    val containerColor = MaterialTheme.colorScheme.onTertiaryContainer
    val contentColor = MaterialTheme.colorScheme.onTertiary
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxWidth().sizeIn(maxHeight = 200.dp).clip(RoundedCornerShape(16.dp))
        .background(
          Brush.verticalGradient(
            colors = listOf(containerColor.copy(alpha = 0.5f), containerColor.copy(alpha = 0.15f)),
          )
        ),
      verticalArrangement = Arrangement.Bottom,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val lastIndex = messages.size - 1
      itemsIndexed(messages, { _, item -> item.index }) { index, message ->
        CustomSnackBar(
          drawer = message,
          onOpen = {
            onSuccess(message.result.data)
          },
          containerColor = containerColor,
          contentColor = contentColor,
          modifier = Modifier.alpha(0.9f),
        )
        if (index != lastIndex) {
          HorizontalDivider()
        }
      }
    }
  }
}

@Composable
internal fun CustomSnackBar(
  drawer: BarcodeResultDrawer,
  onOpen: () -> Unit,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
) {
  // 透明背景
  val clipboardManager = LocalClipboardManager.current
  val mm = LocalWindowMM.current
  val scope = rememberCoroutineScope()
  Box(
    modifier.background(containerColor.copy(alpha = drawer.bgAlphaAni.value)).clickable {
      clipboardManager.setText(AnnotatedString(drawer.result.data))
      scope.launch {
        mm.showToast("已复制到剪切板")
      }
    },
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "${drawer.index}.",
        modifier = Modifier.padding(end = 8.dp),
        color = contentColor,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = drawer.result.data,
        color = contentColor,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      TextButton(
        onClick = onOpen,
        modifier = Modifier.padding(start = 4.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor)
      ) {
        Text(BrowserI18nResource.QRCode.Action())
      }
    }
  }
}