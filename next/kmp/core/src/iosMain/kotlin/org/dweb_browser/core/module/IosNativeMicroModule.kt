package org.dweb_browser.core.module

import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.platform.IPureViewCreateParams
import org.dweb_browser.helper.platform.PureViewController
import platform.UIKit.UIApplication
import kotlin.reflect.KClass

lateinit var nativeMicroModuleUIApplication: UIApplication

fun NativeMicroModule.Companion.getUIApplication() = nativeMicroModuleUIApplication
fun NativeMicroModule.getUIApplication() = nativeMicroModuleUIApplication

private val lockActivityState = Mutex()
fun NativeMicroModule.startUIViewController(
  cls: KClass<PureViewController>,
  buildParams: MutableMap<String, Any?>.() -> Unit
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

class PureViewCreateParams(private val params: Map<String, Any?>) :
  Map<String, Any?> by params, IPureViewCreateParams {
  override fun getString(key: String): String? = get(key).let { require(it is String?);it }
  override fun getInt(key: String): Int? = get(key).let { require(it is Int?);it }
  override fun getFloat(key: String): Float? = get(key).let { require(it is Float?);it }
};
external fun UIApplication.startDelegate(
  delegate: KClass<PureViewController>,
  params: PureViewCreateParams
)
