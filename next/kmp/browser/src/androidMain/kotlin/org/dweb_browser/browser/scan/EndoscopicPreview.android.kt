package org.dweb_browser.browser.scan

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
actual fun EndoscopicPreview(modifier: Modifier, controller: SmartScanController) {
  var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

  val coroutineScope = rememberCoroutineScope()
  val graphicsLayer = rememberGraphicsLayer()
  Box(
    modifier = modifier
      .background(Color.Transparent)
      .drawWithContent {
        // 调用记录以捕获图形层中的内容
        graphicsLayer.record {
          // 将可组合的内容绘制到图形层中
          this@drawWithContent.drawContent()
        }
        // 在可见画布上绘制图形层
        drawLayer(graphicsLayer)
      }
      .clickable {
        coroutineScope.launch {
          imageBitmap = graphicsLayer.toImageBitmap()
        }
      }
  ) {
    imageBitmap?.let {
      Image(
        it, contentDescription = null,
        modifier = Modifier
          .aspectRatio(1f)
          .padding(32.dp),
        contentScale = ContentScale.Crop
      )
    }
  }
}