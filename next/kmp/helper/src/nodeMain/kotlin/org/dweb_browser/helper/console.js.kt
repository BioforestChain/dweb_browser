package org.dweb_browser.helper

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual val ioAsyncExceptionHandler: CoroutineContext =
  Dispatchers.Unconfined + commonAsyncExceptionHandler

actual fun eprintln(message: String) {
  console.error(message)
}