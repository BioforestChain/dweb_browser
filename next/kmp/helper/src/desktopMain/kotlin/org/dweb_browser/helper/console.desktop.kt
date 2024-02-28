package org.dweb_browser.helper

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

actual fun eprintln(message: String) = System.err.println(message)
actual val ioAsyncExceptionHandler: CoroutineContext = Dispatchers.IO + commonAsyncExceptionHandler