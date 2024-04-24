package org.dweb_browser.helper

import kotlinx.coroutines.withContext

actual suspend inline fun <T> withMainContext(crossinline block: suspend () -> T) =
  withContext(mainAsyncExceptionHandler) { block() }
