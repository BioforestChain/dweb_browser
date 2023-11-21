package org.dweb_browser.browser.desk

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.constant.WindowConstants
import org.dweb_browser.sys.window.ext.createRenderer
import org.dweb_browser.sys.window.core.windowAdapterManager

/**
 * 在Desk视角，运行中的Application实例
 * 它一定会有一个主窗口，主窗口如果关闭，那么整个 microModule 也会被关闭（除非它是Service模块，那么允许注册在后台运行，但如果涉及到任何视图层的东西，它必须有主窗口）
 *
 */
class RunningApp(
  /**
   * 这个 ipc 是 running 的标志，这个是我们主动建立的连接，对方如果关闭这个，那么意味着desk会释放掉相关的资源，并且要求dns将它关闭
   */
  val ipc: Ipc,
  val deskNMM: DeskNMM,
  val bootstrapContext: BootstrapContext
) {
  val onClose = ipc.onClose

  /**
   * 所有的窗口实例
   */
  private val windows = mutableListOf<WindowController>()

  /**
   * 创建一个窗口
   */
  private suspend fun createWindow(): WindowController {
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
      mainWin = createWindow().also { win ->
        win.onClose {
          if (mainWin == win) {
            mainWin = null
          }
          bootstrapContext.dns.close(ipc.remote.mmid)
        }
      }
    } else {
      mainWin?.focus()
    }
    mainWin!!
  }

  suspend fun closeMainWindow(force: Boolean = false) {
    getMainWindow().closeRoot(force)
  }
}