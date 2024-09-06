package org.dweb_browser.helper

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

val unconfinedAsyncExceptionHandler: CoroutineContext =
  Dispatchers.Unconfined + commonAsyncExceptionHandler

