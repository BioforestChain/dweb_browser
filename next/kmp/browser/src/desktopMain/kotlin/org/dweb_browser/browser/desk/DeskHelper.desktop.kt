package org.dweb_browser.browser.desk

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.dweb_browser.browser.desk.upgrade.NewVersionItem
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.PureViewCreateParams
import org.dweb_browser.helper.platform.asDesktop
import org.dweb_browser.sys.tray.TrayNMM

private val DeskNMM.DeskRuntime.vcCore by lazy {
  val pvc = PureViewController()
  DesktopViewControllerCore(pvc)
}

actual suspend fun DeskNMM.DeskRuntime.startDesktopView(deskSessionId: String) {
  vcCore.viewController.asDesktop().apply {
    createParams = PureViewCreateParams(mapOf("deskSessionId" to deskSessionId))
    composeWindowParams.title = ""
    composeWindowParams.openWindow()
    composeWindowParams.onCloseRequest = {
      scopeLaunch(cancelable = false) {
        PureViewController.exitDesktop()
      }
    }
//    if (PureViewController.isWindows) {
//      composeWindowParams.undecorated = true
//    }
    scopeLaunch(cancelable = true) {
      val win = awaitComposeWindow()
      // 窗口overlay titlebar
      win.rootPane.apply {
        if (PureViewController.isMacOS) {
          putClientProperty("apple.awt.fullWindowContent", true);
          putClientProperty("apple.awt.transparentTitleBar", true)
        } else if (PureViewController.isWindows) {
//          win.isUndecorated = true;
//          windowDecorationStyle = JRootPane.FRAME;
        }
      }
    }
  }
}

actual suspend fun loadApplicationNewVersion(): NewVersionItem? {
  WARNING("Not yet implement loadNewVersion")
  return null
}

actual fun desktopGridLayout(): GridCells = GridCells.FixedSize(100.dp)

actual fun desktopTap(): Dp = 20.dp

actual fun desktopBgCircleCount(): Int = 12