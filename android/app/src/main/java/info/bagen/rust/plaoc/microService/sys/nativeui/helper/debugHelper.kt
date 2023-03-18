package info.bagen.rust.plaoc.microService.sys.nativeui.helper

import info.bagen.rust.plaoc.microService.helper.printdebugln


inline fun debugNativeUi(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("nativeui", tag, msg, err)
