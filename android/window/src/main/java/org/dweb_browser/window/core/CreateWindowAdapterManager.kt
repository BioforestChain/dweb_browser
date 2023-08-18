package org.dweb_browser.window.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.microservice.help.AdapterManager
import org.dweb_browser.window.core.constant.UUID

typealias CreateWindowAdapter = suspend (winState: WindowState) -> WindowController?

data class WindowRenderScope(val width: Float, val height: Float, val scale: Float)
typealias WindowRenderProvider = @Composable WindowRenderScope.(modifier: Modifier) -> Unit

/**
 * 创建器窗口 的适配器管理
 */
class CreateWindowAdapterManager : AdapterManager<CreateWindowAdapter>() {
  val renderProviders =
    mutableMapOf<UUID, WindowRenderProvider>()

  suspend fun createWindow(winState: WindowState): WindowController {
    for (adapter in adapters) {
      val winCtrl = adapter(winState)
      if (winCtrl != null) {
        /// 窗口创建成功，将窗口保存到实例集合中
        windowInstancesManager.add(winCtrl)
        return winCtrl;
      }
    }
    throw Exception("no support create native window, owner:${winState.owner} provider:${winState.provider}")
  }
}

val createWindowAdapterManager = CreateWindowAdapterManager();


