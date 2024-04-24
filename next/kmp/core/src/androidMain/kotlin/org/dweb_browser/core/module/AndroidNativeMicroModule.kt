package org.dweb_browser.core.module

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.getAppContextUnsafe

private val lockActivityState = Mutex()
fun <T : Activity> MicroModule.Runtime.startAppActivity(
  cls: Class<T>,
  onIntent: (intent: Intent) -> Unit
) {
  scopeLaunch(cancelable = false) {
    lockActivityState.withLock {
      if (grant?.await() == false) {
        return@withLock // TODO 用户拒绝协议应该做的事情ØÏ
      }

      val intent = Intent(getAppContextUnsafe(), cls).also {
        it.`package` = getAppContextUnsafe().packageName
      }
      onIntent(intent)
      getAppContextUnsafe().startActivity(intent)
    }
  }
}