package info.bagen.dwebbrowser.microService.browser.nativeui.helper

import org.dweb_browser.helper.Mmid
import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController

fun NativeUiController.Companion.fromMultiWebView(mmid: Mmid) =
    ((info.bagen.dwebbrowser.microService.mwebview.MultiWebViewNMM.getCurrentWebViewController(mmid)
        ?: throw Exception("native ui is unavailable for $mmid")).lastViewOrNull
        ?: throw Exception("current webview instance is invalid for $mmid")).nativeUiController