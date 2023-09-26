package org.dweb_browser.browserUI.microService.browser.web

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browserUI.R
import org.dweb_browser.browserUI.ui.browser.BrowserViewModel
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.microservice.sys.download.db.DownloadDBStore
import org.dweb_browser.microservice.sys.download.db.createDeskWebLink
import org.dweb_browser.microservice.sys.http.HttpDwebServer
import org.dweb_browser.window.core.WindowController
import org.dweb_browser.window.core.WindowState
import org.dweb_browser.window.core.constant.WindowConstants
import org.dweb_browser.window.core.constant.WindowMode
import org.dweb_browser.window.core.createWindowAdapterManager
import org.dweb_browser.window.core.helper.setFromManifest

class BrowserController(
  private val browserNMM: BrowserNMM, private val browserServer: HttpDwebServer
) {

  private val closeWindowSignal = SimpleSignal()
  val onCloseWindow = closeWindowSignal.toListener()

  private var winLock = Mutex(false)

  suspend fun uninstallWindow() {
    winLock.withLock {
      win?.close(false)
    }
  }

  /**
   * 窗口是单例模式
   */
  private var win: WindowController? = null
  suspend fun openBrowserWindow(search: String? = null, url: String? = null) =
    winLock.withLock<WindowController> {
      viewModel.createNewTab(search, url)
      win?.let { preWin ->
        preWin.state.focus = true
        preWin.state.visible = true
        return preWin
      }
      // 打开安装窗口
      val newWin = createWindowAdapterManager.createWindow(WindowState(
        WindowConstants(
          owner = browserNMM.mmid,
          ownerVersion = browserNMM.version,
          provider = browserNMM.mmid,
          microModule = browserNMM
        )
      ).also { state ->
        state.mode = WindowMode.MAXIMIZE
        state.focus = true // 全屏和focus同时满足，才能显示浮窗而不是侧边栏
        state.setFromManifest(browserNMM)
      })
      newWin.state.closeTip =
        newWin.manager?.state?.viewController?.androidContext?.getString(R.string.browser_confirm_to_close)
          ?: ""
      this.win = newWin
      val wid = newWin.id
      /// 提供渲染适配
      createWindowAdapterManager.renderProviders[wid] = @Composable { modifier ->
        Render(modifier, this)
      }
      /// 窗口销毁的时候
      newWin.onClose {
        closeWindowSignal.emit()
        // 移除渲染适配器
        createWindowAdapterManager.renderProviders.remove(wid)
        ioAsyncScope.cancel()
        win = null
      }
      return newWin
    }

  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler
  val showLoading: MutableState<Boolean> = mutableStateOf(false)
  var viewModel = BrowserViewModel(this, browserNMM, browserServer) { mmid ->
    ioAsyncScope.launch {
      browserNMM.bootstrapContext.dns.open(mmid)
    }
  }

  suspend fun addUrlToDesktop(context: Context, title: String, url: String, icon: Bitmap?) =
    DownloadDBStore.saveWebLink(context, createDeskWebLink(context, title, url, icon))
}