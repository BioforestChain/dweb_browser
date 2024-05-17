package org.dweb_browser.browser.desk

import org.dweb_browser.browser.desk.upgrade.NewVersionItem
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.PureViewCreateParams
import org.dweb_browser.helper.platform.asDesktop
import javax.swing.JFrame
import javax.swing.SwingUtilities


private val DeskNMM.DeskRuntime.vcCore by lazy {
  val pvc = PureViewController()
  DesktopViewControllerCore(pvc)
}

actual suspend fun DeskNMM.DeskRuntime.startDesktopView(deskSessionId: String) {
  vcCore.viewController.asDesktop().apply {
    createParams = PureViewCreateParams(mapOf("deskSessionId" to deskSessionId))
    composeWindowParams.title = "desk.browser.dweb"
//    composeWindowParams.undecorated = true
    composeWindowParams.openWindow()
    composeWindowParams.onCloseRequest = {
      scopeLaunch(cancelable = false) {
        PureViewController.exitDesktop()
      }
    }
//    // 只适用mac
//    debugDesk("isMac=>", " ${System.getProperty("os.name").contains("Mac")}")
//    if (System.getProperty("os.name").contains("Mac")) {
//      awaitComposeWindow().apply {
//        SwingUtilities.invokeLater {
//          val frame = JFrame()
//          val rootPane = frame.rootPane
//          rootPane.putClientProperty("apple.awt.fullWindowContent", true)
//          rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
//          frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
//          frame.setSize(400, 400)
////          frame.add(this)
//          frame.isVisible = composeWindowParams.visible
//        }
//      }
//    }
  }
}

actual suspend fun loadApplicationNewVersion(): NewVersionItem? {
  WARNING("Not yet implement loadNewVersion")
  return null
}