package org.dweb_browser.pure.crypto.cipher

actual suspend fun cipher_aes_256_gcm(
  key: ByteArray,
  data: ByteArray
): ByteArray = common_cipher_aes_256_gcm(key, data)