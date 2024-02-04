package org.dweb_browser.pure.crypto.hash

import kotlinx.coroutines.test.runTest
import org.dweb_browser.pure.crypto.cipher.common_cipher_aes_256_gcm
import org.dweb_browser.pure.crypto.decipher.common_decipher_aes_256_gcm
import kotlin.test.Test
import kotlin.test.assertContentEquals

class Sha256Test {
  @OptIn(ExperimentalUnsignedTypes::class)
  @Test
  fun sha256() = runTest {
    val result = sha256(byteArrayOf(1, 2, 3))
    assertContentEquals(
      result, ubyteArrayOf(
        3U, 144U, 88U, 198U, 242U, 192U, 203U, 73U,
        44U, 83U, 59U, 10U, 77U, 20U, 239U, 119U,
        204U, 15U, 120U, 171U, 204U, 206U, 213U, 40U,
        125U, 132U, 161U, 162U, 1U, 28U, 251U, 129U
      ).toByteArray()
    )
  }

  /**
   * 测试加密和解密后，数据是否一致。
   */
  @Test
  fun sha256Encode() = runTest {
    val result = sha256(byteArrayOf(1, 2, 3))
    println("result = ${result.joinToString()}")
    val encode = common_cipher_aes_256_gcm(sha256("sha256test"), result)
    println("encode = ${encode.joinToString()}")
    val decode = common_decipher_aes_256_gcm(sha256("sha256test"), encode)
    println("decode = ${decode.joinToString()}")
    assertContentEquals(result, decode)
  }
}