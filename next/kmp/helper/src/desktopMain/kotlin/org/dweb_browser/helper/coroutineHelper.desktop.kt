package org.dweb_browser.helper

public actual suspend inline fun <T> withMainContext(crossinline block: suspend () -> T): T =
  withMainContextCommon(block)