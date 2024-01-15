package org.dweb_browser.helper

actual val ioAsyncExceptionHandler = unconfinedAsyncExceptionHandler
actual fun eprintln(message: String) {
  console.error(message)
}