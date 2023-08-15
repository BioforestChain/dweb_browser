package info.bagen.dwebbrowser.microService.browser.nativeui.helper

import org.dweb_browser.helper.printdebugln


inline fun debugNativeUi(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("nativeui", tag, msg, err)
