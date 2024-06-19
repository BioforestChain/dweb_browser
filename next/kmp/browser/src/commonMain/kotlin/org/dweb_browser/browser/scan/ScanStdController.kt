package org.dweb_browser.browser.scan

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.PureViewControllerPlatform
import org.dweb_browser.helper.platform.platform
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.helper.setStateFromManifest
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.getWindow

class ScanStdController(private val scanStdNMM: ScanStdNMM.ScanStdRuntime) {

  private var winLock = Mutex(false)
  private val _scanResult = Signal<String>()
  private val onScanResult = _scanResult.toListener()

  /**
   * 窗口是单例模式
   */
  private var win: WindowController? = null
  suspend fun renderScanWindow(wid: UUID) = winLock.withLock {
    scanStdNMM.getWindow(wid).also { newWin ->
      if (win == newWin) {
        return@withLock
      }
      win = newWin
      newWin.setStateFromManifest(scanStdNMM)

      /// 移动端默认最大化
      // TODO 这里应使用屏幕尺寸来判定
      when (IPureViewController.platform) {
        PureViewControllerPlatform.Android,

        PureViewControllerPlatform.Apple -> {
          newWin.maximize()
        }

        else -> {}
      }

      /// 提供渲染适配
      windowAdapterManager.provideRender(wid) { modifier ->
        ScanStdRender(modifier, this)
      }
      newWin.onClose {
        winLock.withLock {
          if (newWin == win) {
            win = null
          }
        }
        _scanResult.emit("")
      }
    }
  }

  fun callScanResult(result: String) {
    scanStdNMM.scopeLaunch(cancelable = true) {
      _scanResult.emit(result)
      closeWindow()
    }
  }

  fun closeWindow() {
    scanStdNMM.scopeLaunch(cancelable = true) {
      win?.closeRoot()
    }
  }

  suspend fun tryShowScanWindow() = win?.let { winController ->
    val scanResult = CompletableDeferred<String>()
    winController.show()
    val off = onScanResult { scanResult.complete(it) }
    scanResult.await().also { off() }
  }
}