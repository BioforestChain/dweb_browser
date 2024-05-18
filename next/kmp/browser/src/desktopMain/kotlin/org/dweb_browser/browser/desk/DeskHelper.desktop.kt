package org.dweb_browser.browser.desk

import org.dweb_browser.browser.desk.upgrade.NewVersionItem
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.PureViewCreateParams
import org.dweb_browser.helper.platform.asDesktop
import javax.swing.JRootPane

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
          windowDecorationStyle = JRootPane.FRAME;
        }
      }
    }
  }
}

actual suspend fun loadApplicationNewVersion(): NewVersionItem? {
  WARNING("Not yet implement loadNewVersion")
  return null
}