package info.bagen.dwebbrowser.microService.sys.nativeui.helper

import info.bagen.dwebbrowser.microService.helper.Mmid
import info.bagen.dwebbrowser.microService.sys.nativeui.NativeUiController

fun NativeUiController.Companion.fromMultiWebView(mmid: Mmid) =
    ((info.bagen.dwebbrowser.microService.sys.mwebview.MultiWebViewNMM.getCurrentWebViewController(mmid)
        ?: throw Exception("native ui is unavailable for $mmid")).lastViewOrNull
        ?: throw Exception("current webview instance is invalid for $mmid")).nativeUiController