package org.dweb_browser.browser.scan

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.compose.div
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.PureViewControllerPlatform
import org.dweb_browser.helper.platform.platform
import org.dweb_browser.sys.window.core.WindowContentRenderScope

@OptIn(FlowPreview::class)
@Composable
fun WindowContentRenderScope.RenderBarcodeScanning(
  modifier: Modifier, controller: SmartScanController
) {
  // 如果是选中文件
  val selectImg by controller.albumImageFlow.collectAsState(null)
  // 视图切换,如果扫描到了二维码
  Box(modifier) {
    if (selectImg == null) {
      // 渲染相机内容
      CameraPreviewRender(
        modifier = Modifier.fillMaxSize(), controller = controller
      )
      // 扫描线和打开相册，暂时不再桌面端支持
      when (IPureViewController.platform) {
        PureViewControllerPlatform.Desktop -> {
        }

        PureViewControllerPlatform.Apple, PureViewControllerPlatform.Android -> {
          controller.DefaultScanningView(Modifier.fillMaxSize().zIndex(2f))
        }
      }
      // 渲染扫码结果
      controller.RenderScanResultView(
        Modifier.matchParentSize().zIndex(3f)
      )
    } else {
      // 如果是选中图片，渲染选中的图片
      controller.RenderCaptureResult(
        Modifier.fillMaxSize(), selectImg!!
      )
      // 从相册选中的图片，作为背景渲染
//      Image(
//        bitmap = selectImg!!,
//        contentDescription = "Photo",
//        alignment = Alignment.Center,
//        modifier = Modifier,
//      )
//      // 如果没有识别到内容，告知用户
//      val results by controller.barcodeResultFlow.debounce(300).collectAsState(null)
//      if (results?.isEmpty() == true) {
//        controller.RenderEmptyResult()
//      }
    }

  }
}

/**相机preview视图*/
@Composable
expect fun CameraPreviewRender(
  modifier: Modifier = Modifier, controller: SmartScanController
)
