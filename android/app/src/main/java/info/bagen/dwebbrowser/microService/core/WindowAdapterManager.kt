package info.bagen.dwebbrowser.microService.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.dweb_browser.microservice.help.AdapterManager

typealias CreateWindowAdapter = suspend (winState: WindowState) -> WindowController?

class WindowAdapterManager : AdapterManager<CreateWindowAdapter>() {
  val providers =
    mutableMapOf<UUID, @Composable (modifier: Modifier, width: Float, height: Float, scale: Float) -> Unit>()

  suspend fun createWindow(winState: WindowState): WindowController {
    for (adapter in adapters) {
      val winCtrl = adapter(winState)
      if (winCtrl != null) {
        return winCtrl;
      }
    }
    throw Exception("no support create native window, owner:${winState.owner} provider:${winState.provider}")
  }
}

val windowAdapterManager = WindowAdapterManager();


