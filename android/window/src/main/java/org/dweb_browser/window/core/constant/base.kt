package org.dweb_browser.window.core.constant

import org.dweb_browser.helper.printdebugln

fun debugWindow(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("window", tag, msg, err)
typealias UUID = String;