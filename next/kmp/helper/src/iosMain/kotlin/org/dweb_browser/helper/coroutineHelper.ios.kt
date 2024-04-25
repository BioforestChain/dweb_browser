package org.dweb_browser.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual val ioAsyncExceptionHandler = Dispatchers.IO + commonAsyncExceptionHandler
actual suspend inline fun <T> withMainContext(crossinline block: suspend () -> T) =
  withMainContextCommon(block)