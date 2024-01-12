package test

import android.security.keystore.KeyProperties
import javax.crypto.Cipher
import kotlin.test.Test

class CipherTest {
  @Test
  fun doEncryptAndDecrypt() {
    val plaintext = "Hi, 本能"
    val cipher = Cipher.getInstance(
      KeyProperties.KEY_ALGORITHM_AES + "/"
          + KeyProperties.BLOCK_MODE_CBC + "/"
          + KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
    )
    val ciphertext = cipher.doFinal(plaintext.encodeToByteArray())
    println("ciphertext=${ciphertext.decodeToString()}")
    println("plaintext=${cipher.doFinal(ciphertext).decodeToString()}")
  }
}