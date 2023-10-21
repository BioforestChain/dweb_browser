package info.bagen.dwebbrowser.microService.browser.desk

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.constant.WindowConstants
import org.dweb_browser.sys.window.core.createRenderer
import org.dweb_browser.sys.window.core.windowAdapterManager


class RunningApp(
  /**
   * 这个 ipc 是 running 的标志，这个是我们主动建立的连接，对方如果关闭这个，那么意味着desk会释放掉相关的资源，并且要求dns将它关闭
   */
  val ipc: Ipc,
  val deskNMM: DeskNMM,
) {
  val onClose = ipc.onClose

  val windows = mutableListOf<WindowController>()
  suspend fun openWindow(): WindowController {
    val manifest = ipc.remote
    // 打开安装窗口
    val newWin = windowAdapterManager.createWindow(
      WindowState(
        WindowConstants(
          owner = manifest.mmid,
          ownerVersion = manifest.version,
          provider = manifest.mmid,
        )
      )
    )
    windows.add(newWin)

    val wid = newWin.id
    /// 窗口销毁的时候
    newWin.onClose {
      // 移除渲染适配器
      windowAdapterManager.renderProviders.remove(wid)
      // 从引用中移除
      windows.remove(newWin)
    }

    /// 通知模块，提供渲染
    ipc.postMessage(IpcEvent.createRenderer(wid))
    return newWin
  }

  /**
   * 默认只有一个窗口
   */
  private var mainWin: WindowController? = null
  private val openLock = Mutex()

  /**
   * 打开主窗口，默认只会有一个主窗口，重复打开不会重复创建
   */
  suspend fun getMainWindow() = openLock.withLock {
    if (mainWin == null) {
      mainWin = openWindow().also { win ->
        win.onClose {
          if (mainWin == win) {
            mainWin = null
          }
        }
      }
    }
    mainWin!!
  }

}