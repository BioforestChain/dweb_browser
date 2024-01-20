package org.dweb_browser.sys.key

import org.dweb_browser.helper.KeyStore

actual object KeyApi {
  actual fun generatePrivateKey() = KeyStore.generatePrivateKey()

  actual fun encrypt(input: ByteArray) = KeyStore.encrypt(input)

  actual fun decrypt(encryptedData: ByteArray) = KeyStore.decrypt(encryptedData)
}
