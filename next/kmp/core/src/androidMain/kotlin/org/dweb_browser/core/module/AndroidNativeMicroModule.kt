package org.dweb_browser.core.module

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.android.BaseActivity

lateinit var nativeMicroModuleAppContext: Context
fun NativeMicroModule.Companion.getAppContext() = nativeMicroModuleAppContext
fun NativeMicroModule.getAppContext() = nativeMicroModuleAppContext

private val lockActivityState = Mutex()
fun <T : BaseActivity> NativeMicroModule.startAppActivity(
  cls: Class<T>,
  onIntent: (intent: Intent) -> Unit
) {
  ioAsyncScope.launch {
    lockActivityState.withLock {
      if (grant?.waitPromise() == false) {
        return@withLock // TODO 用户拒绝协议应该做的事情ØÏ
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