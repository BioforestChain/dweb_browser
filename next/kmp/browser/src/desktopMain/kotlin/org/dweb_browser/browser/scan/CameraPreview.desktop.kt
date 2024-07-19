package org.dweb_browser.browser.scan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.dweb_browser.browser.common.loading.LoadingView
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter


// 记忆最后一次打开的路径
var lastDirectory = mutableStateOf<String?>(null)

/**
 * 桌面端是选择图片文件
 *
 * @param openAlarmResult 当成功选择图片时的回调
 * @param onBarcodeDetected 当检测到条形码时的回调
 * @param maskView 蒙版视图
 * @param onCancel 取消时的回调
 */
@Composable
actual fun CameraPreviewRender(
  modifier: Modifier,
  controller: SmartScanController
) {
  // 创建渲染动画
  var startAnimation by remember { mutableStateOf(false) }
  val alpha by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = tween(durationMillis = 1000)
  )
  LoadingView(startAnimation) {
  }
  SwingPanel(
    modifier = Modifier.fillMaxSize().alpha(alpha),
    factory = {
      val fileChooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.FILES_ONLY
        fileFilter = FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif", "bmp")
        lastDirectory.value?.let {
          currentDirectory = File(it)
        }
      }
      fileChooser.addActionListener { event ->
        if (event.actionCommand == JFileChooser.APPROVE_SELECTION) {
          lastDirectory.value = fileChooser.currentDirectory.absolutePath
          val byteArray = fileChooser.selectedFile.readBytes()
          controller.imageCaptureFlow.tryEmit(byteArray)
        } else {
          controller.onCancel(event.actionCommand)
        }
      }
      fileChooser
    }
  )
  LaunchedEffect(Unit) {
    startAnimation = true
  }
}