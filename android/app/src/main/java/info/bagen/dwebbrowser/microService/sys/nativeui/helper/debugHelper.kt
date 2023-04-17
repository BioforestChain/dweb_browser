package info.bagen.dwebbrowser.microService.sys.nativeui.helper

import info.bagen.dwebbrowser.microService.helper.printdebugln


inline fun debugNativeUi(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("nativeui", tag, msg, err)
