package org.dweb_browser.browser.scan

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.dweb_browser.browser.common.loading.LoadingView
import org.jetbrains.skia.Image
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter


// 记忆最后一次打开的路径
var lastDirectory = mutableStateOf<String?>(null)

/**
 * 桌面端是选择图片文件
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
actual fun CameraPreviewRender(
  modifier: Modifier,
  controller: SmartScanController
) {
  // 创建渲染动画
  var startLoading by remember { mutableStateOf(true) }

  LoadingView(startLoading) {}


  SwingPanel(
    modifier = Modifier.fillMaxSize(),
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
          val image = Image.makeFromEncoded(byteArray)
          // 回调图片
          controller.albumImageFlow.tryEmit(image.toComposeImageBitmap())
          // 发送给识别模块
          controller.imageCaptureFlow.tryEmit(byteArray)
        } else {
          controller.onCancel(event.actionCommand)
        }
      }
      fileChooser
    }
  )

  LaunchedEffect(Unit) {
    startLoading = false
  }

}