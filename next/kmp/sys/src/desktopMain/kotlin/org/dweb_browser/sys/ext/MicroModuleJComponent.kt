package org.dweb_browser.sys.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.core.windowInstancesManager

suspend fun MicroModule.Runtime.awaitComposeWindow() =
  windowInstancesManager.findByOwner(mmid)?.pureViewController.let {
    require(it is PureViewController);
    it.awaitComposeWindow()
  }

fun MicroModule.Runtime.getComposeWindowOrNull() =
  windowInstancesManager.findByOwner(mmid)?.pureViewController.let {
    require(it is PureViewController);
    it.getComposeWindowOrNull()
  }

fun MicroModule.Runtime.getComposeWindowBoundsOrNull() =
  windowInstancesManager.findByOwner(mmid)?.state?.bounds
