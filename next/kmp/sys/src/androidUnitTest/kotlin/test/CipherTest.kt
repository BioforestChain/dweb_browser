package test

import android.security.keystore.KeyProperties
import org.dweb_browser.helper.toUtf8
import org.dweb_browser.helper.toUtf8ByteArray
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
    val ciphertext = cipher.doFinal(plaintext.toUtf8ByteArray())
    println("ciphertext=${ciphertext.toUtf8()}")
    println("plaintext=${cipher.doFinal(ciphertext).toUtf8()}")
  }
}