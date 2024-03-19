package org.dweb_browser.browser.desk

import org.dweb_browser.helper.platform.PureViewController

actual suspend fun DeskNMM.startDesktopView(deskSessionId: String) {
  PureViewController(mapOf("deskSessionId" to deskSessionId)).windowRender.openWindow()
}