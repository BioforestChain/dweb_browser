package org.dweb_browser.browser.desk

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.sys.window.core.WindowController
import org.dweb_browser.sys.window.core.WindowState
import org.dweb_browser.sys.window.core.constant.WindowConstants
import org.dweb_browser.sys.window.core.windowAdapterManager
import org.dweb_browser.sys.window.ext.createRenderer
import org.dweb_browser.sys.window.ext.createRendererDestroy

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
  val bootstrapContext: BootstrapContext,
  defaultWindowState: WindowState? = null,
) {
  /**
   * 所有的窗口实例
   */
  private val windows = mutableListOf<WindowController>()

  /**
   * 创建一个窗口
   */
  private suspend fun createWindow(referenceState: WindowState? = null): WindowController {
    val manifest = ipc.remote
    // 打开安装窗口
    val newWin = windowAdapterManager.createWindow(
      WindowState(
        WindowConstants(
          owner = manifest.mmid,
          ownerVersion = manifest.version,
          provider = manifest.mmid,
        )
      ).apply {
        if (referenceState != null) {
          colorScheme = referenceState.colorScheme
          alwaysOnTop = referenceState.alwaysOnTop
          keepBackground = referenceState.keepBackground
          mode = referenceState.mode
          updateBounds(referenceState.bounds, WindowState.UpdateReason.Inner)
        }
      }
    )
    windows.add(newWin)
    /// 窗口销毁的时候
    newWin.onClose {
      /// 通知模块，销毁渲染
      if (!ipc.isClosed) {
        ipc.postMessage(IpcEvent.createRendererDestroy(newWin.id))
      }
      // 移除渲染适配器
      windowAdapterManager.renderProviders.remove(newWin.id)
      // 从引用中移除
      windows.remove(newWin)
    }
    val rendererEvent = IpcEvent.createRenderer(newWin.id)
    debugDesk("createWindow") { "rendererEvent=$rendererEvent" }
    ipc.postMessage(rendererEvent)
    return newWin
  }

  /**
   * 默认只有一个窗口
   */
  private var mainWin: WindowController? = null
  private val openLock = Mutex()
  suspend fun getOpenMainWindow() = openLock.withLock { mainWin }

  /**
   * 最后一次窗口的state信息，在重新启动的新窗口的时候，用来参考、继承
   * TODO 这个属性最好持久化存储
   */
  private var latestWindowState: WindowState? = defaultWindowState

  /**
   * 打开主窗口，默认只会有一个主窗口，重复打开不会重复创建
   */
  suspend fun tryOpenMainWindow(visible: Boolean = true) = openLock.withLock {
    if (mainWin == null) {
      latestWindowState?.visible = visible
      mainWin = createWindow(latestWindowState).also { win ->
        latestWindowState = win.state
        win.onClose {
          openLock.withLock {
            if (mainWin == win) {
              mainWin = null
            }
          }
          /// 如果不是允许后台运行，那么主窗口关闭后，也要直接关闭程序
          if (!win.state.keepBackground) {
            bootstrapContext.dns.close(ipc.remote.mmid)
          }
        }
      }
    } else {
      mainWin?.focus()
    }
    mainWin!!
  }


  suspend fun closeMainWindow(force: Boolean = false) {
    tryOpenMainWindow().closeRoot(force)
  }
}