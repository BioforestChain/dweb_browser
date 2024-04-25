package org.dweb_browser.helper

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual val ioAsyncExceptionHandler: CoroutineContext = Dispatchers.IO + commonAsyncExceptionHandler