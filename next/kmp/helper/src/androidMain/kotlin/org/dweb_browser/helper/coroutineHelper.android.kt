package org.dweb_browser.helper

import kotlinx.coroutines.withContext

public actual suspend inline fun <T> withMainContext(crossinline block: suspend () -> T): T =
  withContext(mainAsyncExceptionHandler) { block() }