package org.dweb_browser.core

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.microservice.core.NativeMicroModule

lateinit var nativeMicroModuleAppContext: Context
fun NativeMicroModule.Companion.getAppContext() = nativeMicroModuleAppContext
fun NativeMicroModule.getAppContext() = nativeMicroModuleAppContext

private var grant: PromiseOut<Boolean>? = null
fun NativeMicroModule.Companion.interceptStartAppActivity(granter: PromiseOut<Boolean>) {
  grant = granter
}

private val lockActivityState = Mutex()
fun <T> NativeMicroModule.startAppActivity(cls: Class<T>, onIntent: (intent: Intent) -> Unit) {
  ioAsyncScope.launch {
    lockActivityState.withLock {
      if (grant?.waitPromise() == false) {
        return@withLock // TODO 用户拒绝协议应该做的事情
      }

      val intent = Intent(getAppContext(), cls).also {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        it.`package` = getAppContext().packageName
      }
      onIntent(intent)
      getAppContext().startActivity(intent)
    }
  }
}