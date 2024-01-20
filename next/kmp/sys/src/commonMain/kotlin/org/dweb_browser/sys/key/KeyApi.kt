package org.dweb_browser.sys.key

import org.dweb_browser.sys.biometrics.BiometricsData

expect object KeyApi {
  fun generatePrivateKey()

  fun encrypt(input: ByteArray): ByteArray

  fun decrypt(encryptedData: ByteArray): ByteArray
}