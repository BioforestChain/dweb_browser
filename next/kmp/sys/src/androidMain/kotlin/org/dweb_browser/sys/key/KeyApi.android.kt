package org.dweb_browser.sys.key

import android.app.Application
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.dweb_browser.core.module.getAppContext
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStore.PrivateKeyEntry
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.Cipher

actual object KeyApi {
  private val keyStore = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }

  private val ROOT_KEY_MMID = getAppContext().packageName

  actual fun generatePrivateKey() {
    if(getPrivateKeyEntry() != null) {
      return
    }

    generateKeyPair()
  }

  actual fun encrypt(input: ByteArray): ByteArray = getCipher(Cipher.ENCRYPT_MODE).doFinal(input)

  actual fun decrypt(encryptedData: ByteArray): ByteArray =
    getCipher(Cipher.DECRYPT_MODE).doFinal(encryptedData)

  private fun getCipher(mode: Int): Cipher {
    val privateKeyEntry = getPrivateKeyEntry()!!
    val cipher = Cipher.getInstance(
      KeyProperties.KEY_ALGORITHM_RSA + "/"
          + KeyProperties.BLOCK_MODE_ECB + "/"
          + KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
    )

    cipher.init(
      mode,
      if (mode == Cipher.ENCRYPT_MODE) privateKeyEntry.certificate.publicKey else privateKeyEntry.privateKey
    )

    return cipher
  }

  private fun getPrivateKeyEntry() = keyStore.getEntry(ROOT_KEY_MMID, null) as? PrivateKeyEntry

  private fun generateKeyPair() {
    val keyPairGenerator =
      KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")

    val algorithmParameterSpec: AlgorithmParameterSpec = KeyGenParameterSpec.Builder(
      ROOT_KEY_MMID,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
      .setKeySize(2048)
      .setDigests(KeyProperties.DIGEST_SHA256)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
      .setUserAuthenticationRequired(true)
      .setIsStrongBoxBacked(true)
      // Invalidate the keys if the user has registered a new biometric
      // credential, such as a new fingerprint. Can call this method only
      // on Android 7.0 (API level 24) or higher. The variable
      // "invalidatedByBiometricEnrollment" is true by default.
      .setInvalidatedByBiometricEnrollment(true)
      .build()

    keyPairGenerator.initialize(algorithmParameterSpec)
    keyPairGenerator.generateKeyPair()
  }
}