package org.dweb_browser.core.module

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import platform.UIKit.UIApplication

lateinit var nativeMicroModuleUIApplication: UIApplication

//fun MicroModule.Companion.getUIApplication() = nativeMicroModuleUIApplication
fun MicroModule.Runtime.getUIApplication() = nativeMicroModuleUIApplication

val lockActivityState = Mutex()
fun MicroModule.Runtime.startUIViewController(pureViewController: PureViewController) {
  scopeLaunch(cancelable = false) {
    lockActivityState.withLock {
      if (grant?.await() == false) {
        return@withLock // TODO 用户拒绝协议应该做的事情
      }
      nativeViewController.addOrUpdate(pureViewController)
    }
  }
}
