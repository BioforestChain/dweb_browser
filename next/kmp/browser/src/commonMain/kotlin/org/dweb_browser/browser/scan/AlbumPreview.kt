package org.dweb_browser.browser.scan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.div
import org.dweb_browser.sys.window.core.LocalWindowController

/**选中文件时候的反馈*/
@Composable
fun SmartScanController.RenderAlbumPreview(
  modifier: Modifier,
  selectImg: ImageBitmap,
) {
  LocalWindowController.current.navigation.GoBackHandler {
    albumImageFlow.value = null
    barcodeResultFlow.emit(emptyList()) // 清空缓存的数据
    updatePreviewType(SmartModuleTypes.Scanning) // 切换成扫码模式
  }
  val density = LocalDensity.current.density
  // 显示选中的图片
  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val parentWidth = constraints.maxWidth
    val parentHeight = constraints.maxHeight
    Image(
      bitmap = selectImg,
      contentDescription = "Photo",
      modifier = Modifier.fillMaxSize(),
    )
    //等待识别
    val results by barcodeResultFlow.collectAsState()
    if (results.isEmpty()) {
      RenderEmptyResult()
      return@BoxWithConstraints
    }
    val offsetImgX = (parentWidth - selectImg.width) / 2f
    val offsetImgY = (parentHeight - selectImg.height) / 2f
//    println("offsetImgX:$offsetImgX offsetImgY:$offsetImgY")
    // 画出识别到的内容
    for (result in results) {
      var textSize by remember { mutableStateOf(Size.Zero) }
      val width by animateFloatAsState(result.boundingBox.width / density)
      val height by animateFloatAsState(result.boundingBox.height / density)
      val offsetX by animateFloatAsState(result.boundingBox.x + width - textSize.width)
      val offsetY by animateFloatAsState(result.boundingBox.y + height - textSize.height)
//      println("offset=> $density $offsetX $offsetY ")
      key(result.data) {
        FilledTonalButton(
          { onSuccess(result.data) },
          modifier = Modifier.requiredWidth(width.dp)
            .graphicsLayer {
              translationX = offsetX + offsetImgX
              translationY = offsetY + offsetImgY
            }.onGloballyPositioned { textSize = it.size.div(density) },
          colors = ButtonDefaults.filledTonalButtonColors()
            .run { copy(containerColor = containerColor.copy(alpha = 0.5f)) },
          border = BorderStroke(width = 0.5.dp, brush = SolidColor(Color.White)),
          contentPadding = PaddingValues(horizontal = 4.dp, vertical = 3.dp)
        ) {
          Text(
            result.data,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    }
  }
}

// 识别到空到时候
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartScanController.RenderEmptyResult() {
  val win = LocalWindowController.current
  val uiScope = rememberCoroutineScope()
  BasicAlertDialog(
    onDismissRequest = { },
    modifier = Modifier.clip(AlertDialogDefaults.shape)
      .background(AlertDialogDefaults.containerColor)
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = BrowserI18nResource.QRCode.emptyResult.text,
        modifier = Modifier.padding(vertical = 20.dp)
      )
      Text(
        text = BrowserI18nResource.QRCode.Back(), modifier = Modifier.clickableWithNoEffect {
          uiScope.launch {
            onCancel("back")
//            win.navigation.emitGoBack()
          }
        }.padding(20.dp), color = MaterialTheme.colorScheme.primary
      )
    }
  }
}
