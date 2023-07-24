package info.bagen.dwebbrowser.microService.desktop

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.view.WindowCompat
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.microService.desktop.model.LocalInstallList
import info.bagen.dwebbrowser.microService.desktop.model.LocalOpenList
import info.bagen.dwebbrowser.microService.desktop.ui.DesktopMainView
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme
import kotlinx.coroutines.launch

class DesktopActivity : BaseActivity() {
  private var controller: DesktopController? = null
  private fun upsetRemoteMmid() {
    controller?.activity = null

    controller = DesktopNMM.desktopController?.also { desktopController ->
      desktopController.activity = this
    } ?: throw Exception("no found controller")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    upsetRemoteMmid()

    setContent {
      DwebBrowserAppTheme {
        SideEffect { WindowCompat.setDecorFitsSystemWindows(window, false) }
        val scope = rememberCoroutineScope()
        controller?.let { desktopController ->

          CompositionLocalProvider(
            LocalInstallList provides DesktopNMM.getInstallAppList(),
            LocalOpenList provides DesktopNMM.getRunningAppList(),
          ) {
            DesktopMainView { windowAppInfo ->
              scope.launch {
                desktopController.openApp(windowAppInfo.jsMicroModule.metadata)
              }
            }
          }
        }
      }
    }
  }
}