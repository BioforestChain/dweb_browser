package org.dweb_browser.helper

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

actual val ioAsyncExceptionHandler = Dispatchers.IO + commonAsyncExceptionHandler


@OptIn(ExperimentalForeignApi::class)
val STDERR = platform.posix.fdopen(2, "w")

@OptIn(ExperimentalForeignApi::class)
actual fun eprintln(message: String) {
  platform.posix.fprintf(STDERR, "%s\n", message)
  platform.posix.fflush(STDERR)
}

actual suspend inline fun <T> withMainContext(crossinline block: suspend () -> T) =
  withMainContextCommon(block)