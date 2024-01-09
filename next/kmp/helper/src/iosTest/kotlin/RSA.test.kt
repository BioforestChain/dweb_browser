import kotlinx.cinterop.ExperimentalForeignApi
import org.dweb_browser.helper.RSA
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test


class RSACryptoTest {
  @OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
  @Test
  fun `rsa_crypt_test`() {
    val plainText = "123456"
    val (publicKey, privateKey) = RSA.generatePublicKeyAndPrivateKey()
    val encryptedData = RSA.encryptData(publicKey!!, plainText)
    val actual = RSA.decryptData(privateKey!!, encryptedData)
    assert(plainText == actual)
  }
}
