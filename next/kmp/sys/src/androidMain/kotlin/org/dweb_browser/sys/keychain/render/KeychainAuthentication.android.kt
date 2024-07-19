package org.dweb_browser.sys.keychain.render

import android.os.Build

internal actual fun getDeviceName() = "${Build.BRAND}(power by Android)"

/**
 * 获取已经注册的认证方案
 */
internal actual fun getRegisteredMethod(): KeychainMethod? = getCustomRegisteredMethod()
internal actual fun getSupportMethods(): List<KeychainMethod> =
  listOf(KeychainMethod.Password)