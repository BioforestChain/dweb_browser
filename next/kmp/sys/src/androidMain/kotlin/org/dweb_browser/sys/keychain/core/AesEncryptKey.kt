package org.dweb_browser.sys.keychain.core

import android.os.Build
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import kotlinx.serialization.Serializable
import org.dweb_browser.sys.biometrics.BiometricsActivity
import org.dweb_browser.sys.biometrics.BiometricsI18nResource
import org.dweb_browser.sys.keychain.ANDROID_KEY_STORE
import org.dweb_browser.sys.keychain.KeychainI18nResource
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec

@Serializable
abstract class AesEncryptKey(private val transformation: String) : EncryptKey() {
  companion object {
    const val IV_SIZE_BYTES = 16 // For AES in CBC or GCM mode
  }

  abstract suspend fun readCryptKey(params: UseKeyParams): SecretKey

  private fun getKeyInfo(cryptKey: SecretKey): KeyInfo? = runCatching {
    (SecretKeyFactory.getInstance(cryptKey.algorithm, ANDROID_KEY_STORE)
      .getKeySpec(cryptKey, KeyInfo::class.java) as KeyInfo).also { keyInfo ->
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val securityLevel = when (val securityLevel = keyInfo.securityLevel) {
          KeyProperties.SECURITY_LEVEL_UNKNOWN -> "unknown"
          KeyProperties.SECURITY_LEVEL_UNKNOWN_SECURE -> "unknown_secure"
          KeyProperties.SECURITY_LEVEL_SOFTWARE -> "software"
          KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT -> "trusted_environment"
          KeyProperties.SECURITY_LEVEL_STRONGBOX -> "strongbox"
          else -> "invalid securityLevel:$securityLevel"
        }
        println("QAQ keyInfo.securityLevel=${securityLevel}")
      } else {
        println("QAQ keyInfo.isInsideSecureHardware=${keyInfo.isInsideSecureHardware}")
      }
    }
  }.getOrNull()

  /**
   * 生物识别
   */
  private suspend fun checkBiometrics(params: UseKeyParams, cryptKey: SecretKey, cipher: Cipher) {
    val keyInfo = getKeyInfo(cryptKey)
    if (keyInfo != null && keyInfo.isUserAuthenticationRequired && keyInfo.userAuthenticationValidityDurationSeconds <= 0) {
      val biometricsActivity = BiometricsActivity.create(params.runtime)
      biometricsActivity.waitSupportOrThrow()
      biometricsActivity.authenticateWithClass3BiometricsDeferred(
        crypto = BiometricPrompt.CryptoObject(cipher),
        title = KeychainI18nResource.name.text,
        negativeButtonText = BiometricsI18nResource.cancel_button.text,
        subtitle = KeychainI18nResource.require_auth_subtitle.text,
        description = params.remoteMmid,
        confirmationRequired = true,
      )
    }
  }

  /**
   * 数据加密
   */
  override suspend fun encryptData(params: UseKeyParams, sourceData: ByteArray): ByteArray {
    val cryptKey = readCryptKey(params)
    val cipher: Cipher = Cipher.getInstance(transformation)
    cipher.init(Cipher.ENCRYPT_MODE, cryptKey)
    checkBiometrics(params, cryptKey, cipher)

    val encryptedData = cipher.doFinal(sourceData)
    return cipher.iv + encryptedData
  }

  /**
   * 数据解密
   */
  override suspend fun decryptData(params: UseKeyParams, encryptedBytes: ByteArray): ByteArray {
    val cipher = Cipher.getInstance(transformation)
    val iv = encryptedBytes.sliceArray(0..<IV_SIZE_BYTES)
    val encryptedData = encryptedBytes.sliceArray(IV_SIZE_BYTES..<encryptedBytes.size)
    val spec = IvParameterSpec(iv)
    val cryptKey = readCryptKey(params)
    cipher.init(Cipher.DECRYPT_MODE, cryptKey, spec)
    checkBiometrics(params, cryptKey, cipher)

    return cipher.doFinal(encryptedData)
  }
}