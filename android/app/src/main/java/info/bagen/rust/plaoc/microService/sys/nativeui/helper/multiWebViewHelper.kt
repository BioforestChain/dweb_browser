package info.bagen.rust.plaoc.microService.sys.nativeui

import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.sys.mwebview.MultiWebViewNMM

fun NativeUiController.Companion.fromMultiWebView(mmid: Mmid) =
    ((MultiWebViewNMM.getCurrentWebViewController(mmid)
        ?: throw Exception("native ui is unavailable for $mmid")).lastViewOrNull
        ?: throw Exception("current webview instance is invalid for $mmid")).nativeUiController