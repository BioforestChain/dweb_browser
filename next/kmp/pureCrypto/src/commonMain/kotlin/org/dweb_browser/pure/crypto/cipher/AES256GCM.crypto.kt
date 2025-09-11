package org.dweb_browser.pure.crypto.cipher

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES

class AES256GCM {
  companion object {
    // 使用 SymmetricKeySize.B256 的长度作为key
    private val aesGcm = CryptographyProvider.Default.get(AES.GCM)
    private val keyDecoder = aesGcm.keyDecoder()

    /**
     * 将指定字符串，通过 sha256 转成合法的 AES.GCM 的 key
     */
    internal suspend fun getCipher(key: ByteArray) = keyDecoder.decodeFromByteArray(
      AES.Key.Format.RAW, key
    ).cipher()
  }
}

suspend fun common_cipher_aes_256_gcm(
  key: ByteArray,
  data: ByteArray
): ByteArray {
  return AES256GCM.getCipher(key).encrypt(data)
}