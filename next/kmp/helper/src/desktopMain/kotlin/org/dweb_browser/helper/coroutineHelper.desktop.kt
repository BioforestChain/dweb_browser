package org.dweb_browser.helper

actual suspend inline fun <T> withMainContext(crossinline block: suspend () -> T) =
  withMainContextCommon(block)