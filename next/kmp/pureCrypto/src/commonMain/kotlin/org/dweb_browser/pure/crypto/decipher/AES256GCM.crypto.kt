package org.dweb_browser.pure.crypto.decipher

import org.dweb_browser.pure.crypto.cipher.AES256GCM

suspend fun common_decipher_aes_256_gcm(
  key: ByteArray,
  data: ByteArray
): ByteArray {
  return AES256GCM.getCipher(key).decrypt(data)
}