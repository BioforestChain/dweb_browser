package org.dweb_browser.helper

import kotlin.coroutines.CoroutineContext

actual val ioAsyncExceptionHandler: CoroutineContext
  get() = TODO("Not yet implemented")

actual fun eprintln(message: String) {
  console.error(message)
}