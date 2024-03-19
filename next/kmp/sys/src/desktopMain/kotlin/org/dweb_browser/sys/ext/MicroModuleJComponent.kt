package org.dweb_browser.sys.ext

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.sys.window.core.windowInstancesManager

suspend fun MicroModule.awaitComposeWindow() =
  windowInstancesManager.findByOwner(mmid)?.pureViewController.let {
    require(it is PureViewController);
    it.awaitComposeWindow()
  }

fun MicroModule.getComposeWindowOrNull() =
  windowInstancesManager.findByOwner(mmid)?.pureViewController.let {
    require(it is PureViewController);
    it.getComposeWindowOrNull()
  }
