package org.dweb_browser.helper

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlin.coroutines.CoroutineContext

public actual val ioAsyncExceptionHandler: CoroutineContext =
  Dispatchers.IO + commonAsyncExceptionHandler

public actual suspend inline fun <T> withMainContext(crossinline block: suspend () -> T): T =
  withMainContextCommon(block)