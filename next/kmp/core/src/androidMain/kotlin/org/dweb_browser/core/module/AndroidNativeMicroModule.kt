package org.dweb_browser.core.module

import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

lateinit var nativeMicroModuleAppContext: Context
fun getAppContext() = nativeMicroModuleAppContext

private val lockActivityState = Mutex()
fun <T : Activity> MicroModule.Runtime.startAppActivity(
  cls: Class<T>,
  onIntent: (intent: Intent) -> Unit
) {
  scopeLaunch {
    lockActivityState.withLock {
      if (grant?.await() == false) {
        return@withLock // TODO 用户拒绝协议应该做的事情ØÏ
      }

      val intent = Intent(getAppContext(), cls).also {
        it.`package` = getAppContext().packageName
      }
      onIntent(intent)
      getAppContext().startActivity(intent)
    }
  }
}