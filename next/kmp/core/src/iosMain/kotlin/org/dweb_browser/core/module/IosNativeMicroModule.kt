package org.dweb_browser.core.module

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.platform.IPureViewController
import org.dweb_browser.helper.platform.IPureViewCreateParams
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.platform.PureViewCreateParams
import platform.UIKit.UIApplication
import kotlin.reflect.KClass

lateinit var nativeMicroModuleUIApplication: UIApplication

fun NativeMicroModule.Companion.getUIApplication() = nativeMicroModuleUIApplication
fun NativeMicroModule.getUIApplication() = nativeMicroModuleUIApplication

private val lockActivityState = Mutex()
fun <T : IPureViewController> NativeMicroModule.startUIViewController(
  cls: KClass<T>,
  buildParams: MutableMap<String, Any?>.() -> Unit = {}
) {
  ioAsyncScope.launch {
    lockActivityState.withLock {
      if (grant?.waitPromise() == false) {
        return@withLock // TODO 用户拒绝协议应该做的事情
      }

      val params = PureViewCreateParams(mutableMapOf<String, Any?>().also(buildParams))
      getUIApplication().startDelegate(cls, params)
    }
  }
}

external fun <T : IPureViewController> UIApplication.startDelegate(
  delegate: KClass<T>,
  params: PureViewCreateParams
)
