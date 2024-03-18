package org.dweb_browser.helper

import android.os.Looper
import kotlinx.coroutines.withContext

actual suspend inline fun <T> withMainContext(crossinline block: suspend () -> T) =
  if (Looper.getMainLooper().isCurrentThread) {
    block()
  } else withContext(mainAsyncExceptionHandler) { block() }
