package org.dweb_browser.browser.scan

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.PureViewControllerPlatform
import org.dweb_browser.helper.platform.platform
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getMainWindow
import org.dweb_browser.sys.window.ext.getMainWindowId
import org.dweb_browser.sys.window.ext.getOrOpenMainWindow

class ScanStdController(private val scanStdNMM: ScanStdNMM.ScanStdRuntime) {

  private val viewDeferredFlow =
    MutableStateFlow(CompletableDeferred<WindowController>())
  private val viewDeferred get() = viewDeferredFlow.value
  private val winLock = Mutex()

  /**
   * 创建窗口控制器
   */
  suspend fun getWindowController() = winLock.withLock {
    if (viewDeferred.isCompleted) {
      val controller = viewDeferred.getCompleted()
      if (controller.id == scanStdNMM.getMainWindowId()) {
        return@withLock controller
      }
      viewDeferredFlow.value = CompletableDeferred()
    }
    scanStdNMM.getMainWindow().also { newController ->
      viewDeferred.complete(newController)
      newController.setStateFromManifest(scanStdNMM)
      newController.state.alwaysOnTop = true // 扫码模块置顶
      /// 提供渲染适配
      windowAdapterManager.provideRender(newController.id) { modifier ->
        Render(modifier, this)
      }
      // 适配各个平台样式 移动端默认最大化
      when (IPureViewController.platform) {
        PureViewControllerPlatform.Android,
        PureViewControllerPlatform.Apple -> {
          newController.maximize()
        }

        else -> {}
      }
      newController.onClose {
        viewDeferredFlow.value = CompletableDeferred()
      }
    }
  }

  // 返回扫码结果
  var saningResult = CompletableDeferred<String>()

  /**扫码成功*/
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

  fun closeWindow() {
    scanStdNMM.scopeLaunch(cancelable = true) {
      if (canCloseWindow) {
        scanStdNMM.getOrOpenMainWindow().closeRoot()
      }
    }
  }
}