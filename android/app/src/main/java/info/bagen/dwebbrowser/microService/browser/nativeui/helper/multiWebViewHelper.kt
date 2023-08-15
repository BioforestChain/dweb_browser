package info.bagen.dwebbrowser.microService.browser.nativeui.helper

import info.bagen.dwebbrowser.microService.browser.nativeui.NativeUiController
import org.dweb_browser.microservice.help.MMID

fun NativeUiController.Companion.fromMultiWebView(mmid: MMID): NativeUiController =
  throw Exception("native ui is unavailable for $mmid")
//    ((MultiWebViewNMM.getCurrentWebViewController(mmid)
//        ?: throw Exception("native ui is unavailable for $mmid")).lastViewOrNull
//        ?: throw Exception("current webview instance is invalid for $mmid")).nativeUiController