package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.isMobile
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowSurface
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindowId
import org.dweb_browser.sys.window.ext.getOrOpenMainWindow

class SmartScanController(
  private val smartScanNMM: SmartScanNMM.ScanRuntime,
  internal val scanningController: ScanningController
) {

  private val viewDeferredFlow = MutableStateFlow(CompletableDeferred<WindowController>())
  private val viewDeferred get() = viewDeferredFlow.value
  private val winLock = Mutex()
  var previewTypes by mutableStateOf(SmartModuleTypes.Scanning)
    private set

  fun updatePreviewType(type: SmartModuleTypes) {
    previewTypes = type
  }

  // 用来跟ios形成视图绘画对冲
  internal val scaleFlow = MutableStateFlow(1f)

  /**
   * 创建窗口控制器
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  suspend fun getWindowController() = winLock.withLock {
    if (viewDeferred.isCompleted) {
      val controller = viewDeferred.getCompleted()
      if (controller.id == smartScanNMM.getMainWindowId()) {
        return@withLock controller
      }
      viewDeferredFlow.value = CompletableDeferred()
    }

    smartScanNMM.getOrOpenMainWindow().also { newController ->
      viewDeferred.complete(newController)
      newController.setStateFromManifest(smartScanNMM)
      newController.state.resizable = false // 禁止 resize
      newController.state.alwaysOnTop = true // 扫码模块置顶
      /// 提供渲染适配
      windowAdapterManager.provideRender(newController.id) { modifier ->
        // 智能扫码
        this@SmartScanController.scaleFlow.value = scale
        WindowSurface(modifier) {
          RenderBarcodeScanning(
            modifier = Modifier.fillMaxSize(),
            controller = this@SmartScanController
          )
        }
        // AI识物
      }

      // 适配各个平台样式 移动端默认最大化
      when {
        IPureViewController.isMobile -> {
          newController.fullscreen()
        }

        else -> {}
      }
      newController.onHidden {
        onCancel("onHidden")
      }
      newController.onClose {
        viewDeferredFlow.value = CompletableDeferred()
      }
    }
  }

  // 返回识别结果
  var saningResult = CompletableDeferred<String>()

  // 拿到的解码流
  val barcodeResultFlow = MutableStateFlow<List<BarcodeResult>>(emptyList())

  // 相册选中的图片
  val albumImageFlow = MutableStateFlow<ByteArray?>(null)

  /**识别成功*/
  fun onSuccess(result: String) {
    saningResult.complete(result)
    saningResult = CompletableDeferred()
    closeWindow()
  }

  fun onCancel(reason: String) {
    saningResult.cancel(reason)
    saningResult = CompletableDeferred()
    closeWindow()
  }

  private val canCloseWindow get() = viewDeferred.isCompleted

  private fun closeWindow() {
    cameraController?.stop()  // 暂停
    smartScanNMM.scopeLaunch(cancelable = true) {
      if (canCloseWindow) {
        smartScanNMM.getOrOpenMainWindow().closeRoot()
      }
    }
  }

  /**解码二维码*/
  suspend fun decodeQrCode(processor: suspend ScanningController.() -> List<BarcodeResult>) {
    barcodeResultFlow.value = scanningController.processor()
  }

  // 相机控制器
  var cameraController: CameraController? = null
}


interface CameraController {
  fun toggleTorch()
  fun openAlbum()
  fun stop()
}

enum class SmartModuleTypes {
  Scanning,
  Album,
  Endoscopic
}