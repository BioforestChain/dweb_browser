package org.dweb_browser.pure.crypto.decipher

actual suspend fun decipher_aes_256_gcm(
  key: ByteArray,
  data: ByteArray
): ByteArray = common_decipher_aes_256_gcm(key, data)
