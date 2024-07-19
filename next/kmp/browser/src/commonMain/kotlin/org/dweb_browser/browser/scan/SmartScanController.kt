package org.dweb_browser.browser.scan

import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.PureViewControllerPlatform
import org.dweb_browser.helper.platform.platform
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.core.withRenderScope
import org.dweb_browser.sys.window.ext.getMainWindowId
import org.dweb_browser.sys.window.ext.getOrOpenMainWindow

class SmartScanController(
  private val smartScanNMM: SmartScanNMM.ScanRuntime, private val scanningManager: ScanningManager
) {

  private val viewDeferredFlow = MutableStateFlow(CompletableDeferred<WindowController>())
  private val viewDeferred get() = viewDeferredFlow.value
  private val winLock = Mutex()

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
      newController.state.alwaysOnTop = true // 扫码模块置顶
      /// 提供渲染适配
      windowAdapterManager.provideRender(newController.id) { modifier ->
        // 智能扫码
        this@SmartScanController.scaleFlow.value = scale
        RenderBarcodeScanning(
          modifier = modifier.withRenderScope(this).fillMaxSize(),
          controller = this@SmartScanController
        )
        // AI识物
      }

      // 适配各个平台样式 移动端默认最大化
      when (IPureViewController.platform) {
        PureViewControllerPlatform.Android, PureViewControllerPlatform.Apple -> {
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

  // 监听相机来的图片流
  val imageCaptureFlow = MutableSharedFlow<Any>(extraBufferCapacity = 1).also { flow ->
    flow.collectIn(scope = smartScanNMM.getRuntimeScope()) { byteArray ->
      val result = decodeQrCode(byteArray)
//      debugSCAN("decodeQrCode=>${result.size}", "size=>${byteArray}")
      barcodeResultFlow.emit(result)
    }
  }

  // 拿到的解码流
  val barcodeResultFlow = MutableStateFlow<List<BarcodeResult>>(emptyList())

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
  private suspend fun decodeQrCode(data: Any): List<BarcodeResult> {
    val result = try {
      scanningManager.recognize(data, 0)
    } catch (e: Throwable) {
      WARNING("decode error=>${e.message}")
      emptyList()
    }
    return result
  }

  // 相机控制器
  var cameraController: CameraController? = null
}


interface CameraController {
  fun toggleTorch()
  fun openAlbum()
  fun stop()
}