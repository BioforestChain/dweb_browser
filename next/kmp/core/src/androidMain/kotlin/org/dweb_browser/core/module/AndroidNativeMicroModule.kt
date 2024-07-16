package org.dweb_browser.core.module

import android.app.Activity
import android.content.Intent
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.getAppContext
import org.dweb_browser.helper.getStartActivityOptions
import org.dweb_browser.helper.withMainContext

private val lockActivityState = Mutex()

suspend fun <T : Activity> MicroModule.Companion.startAppActivity(
  cls: Class<T>,
  onIntent: (intent: Intent) -> Unit,
) = lockActivityState.withLock {
  if (grant?.await() == false) {
    return@withLock // TODO 用户拒绝协议应该做的事情ØÏ
  }
  val (context, optionsBuilder) = getStartActivityOptions() ?: (getAppContext() to null)
  val intent = Intent(context, cls).also {
    it.`package` = context.packageName
  }
  onIntent(intent)

  withMainContext {
    context.startActivity(intent, optionsBuilder?.invoke()) // startActivity 需要放在主线程
  }
}

fun <T : Activity> MicroModule.Runtime.startAppActivity(
  cls: Class<T>,
  onIntent: (intent: Intent) -> Unit,
): Job = scopeLaunch(cancelable = false) {
  MicroModule.startAppActivity(cls, onIntent)
}

suspend fun MicroModule.Companion.startAppActivity(
  intent: Intent,
) = lockActivityState.withLock {
  if (grant?.await() == false) {
    return@withLock // TODO 用户拒绝协议应该做的事情ØÏ
  }
  val (context, optionsBuilder) = getStartActivityOptions() ?: (getAppContext() to null)

  withMainContext {
    context.startActivity(intent, optionsBuilder?.invoke()) // startActivity 需要放在主线程
  }
}

fun MicroModule.Runtime.startAppActivity(intent: Intent) = scopeLaunch(cancelable = false) {
  MicroModule.startAppActivity(intent)
}