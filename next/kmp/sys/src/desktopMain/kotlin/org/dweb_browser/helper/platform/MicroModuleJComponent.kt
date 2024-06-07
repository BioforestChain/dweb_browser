package org.dweb_browser.helper.platform

import androidx.compose.ui.awt.ComposeWindow
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.toPureIntRect
import org.dweb_browser.sys.window.core.windowInstancesManager

suspend fun MicroModule.Runtime.awaitComposeWindow() =
  windowInstancesManager.findByOwner(mmid)?.pureViewController.let {
    require(it is PureViewController);
    it.awaitComposeWindow()
  }

fun MicroModule.Runtime.getComposeWindowOrNull() = (getPureViewControllerOrNull()
  ?: windowInstancesManager.findByOwner(mmid)?.pureViewController
  ?: getRootPureViewControllerOrNull())?.let {
  require(it is PureViewController);
  it.getComposeWindowOrNull()
}

fun MicroModule.Runtime.getComposeWindowBoundsOrNull(composeWindow: ComposeWindow? = getComposeWindowOrNull()) =
  composeWindow?.run { bounds.toPureIntRect() }
