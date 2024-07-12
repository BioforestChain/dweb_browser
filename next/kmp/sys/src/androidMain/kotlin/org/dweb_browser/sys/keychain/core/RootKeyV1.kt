package org.dweb_browser.sys.keychain.core

import android.security.keystore.KeyProperties
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class RootKeyV1(private val encoded: ByteArray) : AesEncryptKey(RootKeyV1.TRANSFORMATION) {
  companion object {
    const val VERSION = "v1"
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_HMAC_SHA256// KEY_ALGORITHM_AES
    private const val BLOCK_MODES = KeyProperties.BLOCK_MODE_CBC  // 目前支持的模式里，算是最安全的；它的缺点是不能并发处理
    private const val ENCRYPTION_PADDINGS = KeyProperties.ENCRYPTION_PADDING_PKCS7
    private const val TRANSFORMATION = "AES/$BLOCK_MODES/$ENCRYPTION_PADDINGS"
  }

  private val cryptKey by lazy { SecretKeySpec(encoded, ALGORITHM) }
  override suspend fun readCryptKey(params: UseKeyParams): SecretKey {
    return cryptKey
  }
}