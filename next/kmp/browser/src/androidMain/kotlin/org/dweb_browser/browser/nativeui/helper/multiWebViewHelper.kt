package org.dweb_browser.browser.nativeui.helper

import org.dweb_browser.browser.nativeui.NativeUiController
import org.dweb_browser.microservice.help.types.MMID

fun org.dweb_browser.browser.nativeui.NativeUiController.Companion.fromMultiWebView(mmid: MMID): org.dweb_browser.browser.nativeui.NativeUiController =
  throw Exception("native ui is unavailable for $mmid")
//    ((MultiWebViewNMM.getCurrentWebViewController(mmid)
//        ?: throw Exception("native ui is unavailable for $mmid")).lastViewOrNull
//        ?: throw Exception("current webview instance is invalid for $mmid")).nativeUiController