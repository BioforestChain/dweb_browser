package org.dweb_browser.core.module

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.platform.NativeViewController.Companion.nativeViewController
import org.dweb_browser.helper.platform.PureViewController
import platform.UIKit.UIApplication

lateinit var nativeMicroModuleUIApplication: UIApplication

fun NativeMicroModule.Companion.getUIApplication() = nativeMicroModuleUIApplication
fun NativeMicroModule.getUIApplication() = nativeMicroModuleUIApplication

val lockActivityState = Mutex()
fun NativeMicroModule.startUIViewController(pureViewController: PureViewController) {
  ioAsyncScope.launch {
    lockActivityState.withLock {
      if (grant?.await() == false) {
        return@withLock // TODO 用户拒绝协议应该做的事情
      }
      nativeViewController.addOrUpdate(pureViewController)
    }
  }
}
