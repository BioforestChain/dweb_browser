package org.dweb_browser.browser.nativeui.helper

import org.dweb_browser.browser.mwebview.MultiWebViewNMM
import org.dweb_browser.browser.nativeui.NativeUiController
import org.dweb_browser.core.help.types.MMID

fun NativeUiController.Companion.fromMultiWebView(mmid: MMID): NativeUiController =
  ((MultiWebViewNMM.getCurrentWebViewController(mmid)
    ?: throw Exception("native ui is unavailable for $mmid")).lastViewOrNull
    ?: throw Exception("current webview instance is invalid for $mmid")).nativeUiController
    ?: throw Exception("nativeUiController is null for $mmid")