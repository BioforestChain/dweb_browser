package org.dweb_browser.browser.desk

import org.dweb_browser.browser.desk.upgrade.NewVersionItem
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.PureViewCreateParams
import org.dweb_browser.helper.platform.asDesktop

private val DeskNMM.DeskRuntime.vcCore by lazy {
  val pvc = PureViewController()
  DesktopViewControllerCore(pvc)
}

actual suspend fun DeskNMM.DeskRuntime.startDesktopView(deskSessionId: String) {
  vcCore.viewController.asDesktop().apply {
    createParams = PureViewCreateParams(mapOf("deskSessionId" to deskSessionId))
    composeWindowParams.title = "desk.browser.dweb"
    composeWindowParams.openWindow()
    composeWindowParams.onCloseRequest = {
      scopeLaunch(cancelable = false) {
        PureViewController.exitDesktop()
      }
    }
  }
}

actual suspend fun loadApplicationNewVersion(): NewVersionItem? {
  WARNING("Not yet implement loadNewVersion")
  return null
}