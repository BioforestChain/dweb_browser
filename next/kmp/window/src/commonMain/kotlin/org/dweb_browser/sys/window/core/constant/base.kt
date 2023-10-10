package org.dweb_browser.sys.window.core.constant

import org.dweb_browser.helper.printDebug

fun debugWindow(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("window", tag, msg, err)
