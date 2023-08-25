package info.bagen.dwebbrowser.microService.browser.nativeui.helper

import org.dweb_browser.helper.printDebug


inline fun debugNativeUi(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("nativeui", tag, msg, err)
