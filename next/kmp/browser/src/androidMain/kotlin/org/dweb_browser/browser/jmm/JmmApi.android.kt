package org.dweb_browser.browser.jmm

import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.consumeEachArrayRange
import java.security.MessageDigest

actual fun getChromeWebViewVersion(): String? {
  WebView.getCurrentWebViewPackage()?.let { webViewPackage -> // 获取当前WebView版本号
    // 这边过滤华为的webview版本：com.huawei.webview,,,android.webkit.webview
    if (webViewPackage.packageName == "com.google.android.webview") {
      return webViewPackage.versionName // 103.0.5060.129
    }
  }
  return null
}

actual suspend fun jmmAppHashVerify(jmmNMM: JmmNMM, jmmHistoryMetadata: JmmHistoryMetadata, zipFilePath: String): Boolean {
  val messageDigest = MessageDigest.getInstance("SHA-256")
  val deferred = CompletableDeferred<String>()
  jmmNMM.nativeFetch("file://file.std.dweb/read?path=$zipFilePath").stream().getReader("JmmAppHashVerify").consumeEachArrayRange { byteArray, last ->
    if(!last) {
      messageDigest.update(byteArray)
    } else {
      val hashValue = messageDigest.digest().joinToString("") { "%02x".format(it) }
      debugJMM("jmmAppHashVerify", "bundleHash=${jmmHistoryMetadata.metadata.bundle_hash}")
      debugJMM("jmmAppHashVerify", "zipFileHash=sha256:${hashValue}")
      deferred.complete(hashValue)
    }
  }

  return "sha256:${deferred.await()}" == jmmHistoryMetadata.metadata.bundle_hash
}