package org.dweb_browser.sys.keychain.render


internal actual fun getDeviceName(): String = "iOS"

/**
 * 获取已经注册的认证方案
 */
internal actual fun getRegisteredMethod(): KeychainMethod? = KeychainMethod.Biometrics
internal actual fun getSupportMethods(): List<KeychainMethod> =
  listOf(KeychainMethod.Biometrics)