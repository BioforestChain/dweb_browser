package org.dweb_browser.browser.jmm

import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.withMainContext

actual fun getChromeWebViewVersion(): String? {
  WebView.getCurrentWebViewPackage()?.let { webViewPackage -> // 获取当前WebView版本号
    // 这边过滤华为的webview版本：com.huawei.webview,,,android.webkit.webview
    if (webViewPackage.packageName == "com.google.android.webview") {
      return webViewPackage.versionName // 103.0.5060.129
    }
  }
  return null
}