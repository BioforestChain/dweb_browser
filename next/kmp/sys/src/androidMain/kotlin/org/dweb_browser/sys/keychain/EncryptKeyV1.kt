package org.dweb_browser.sys.keychain

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.utf8Binary
import org.dweb_browser.sys.biometrics.BiometricCheckResult
import org.dweb_browser.sys.biometrics.BiometricsActivity
import org.dweb_browser.sys.biometrics.BiometricsI18nResource
import org.dweb_browser.sys.biometrics.BiometricsManage
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
@SerialName("v1")
class EncryptKeyV1(
  val encoded: ByteArray,
  val algorithm: String,
) : EncryptKey {
  companion object {
    private const val alias = "keychainstore-key-v1"
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODES = KeyProperties.BLOCK_MODE_CBC  // 目前支持的模式里，算是最安全的；它的缺点是不能并发处理
    private const val ENCRYPTION_PADDINGS = KeyProperties.ENCRYPTION_PADDING_PKCS7

    //private const val ENCRYPTION_PADDINGS = KeyProperties.ENCRYPTION_PADDING_NONE
//private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val TRANSFORMATION = "$ALGORITHM/$BLOCK_MODES/$ENCRYPTION_PADDINGS"
    private const val IV_SIZE_BYTES = 16 // For AES in CBC or GCM mode

    @OptIn(ExperimentalSerializationApi::class)
    internal val getOrRecoveryKey: GetOrRecoveryKey = { runtime ->
      (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { secretKey ->
        EncryptKeyV1(secretKey.encoded, secretKey.algorithm)
      } ?: deviceKeyStore.getRawItem(alias.utf8Binary)
        ?.let { Cbor.decodeFromByteArray<EncryptKeyV1>(it) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    internal val generateKey: GenerateKey = { runtime ->
      val secretKey = createKey(runtime)
      EncryptKeyV1(secretKey.encoded, secretKey.algorithm).also {
        deviceKeyStore.setRawItem(alias.utf8Binary, Cbor.encodeToByteArray(it))
      }
    }

    private suspend fun createKey(runtime: MicroModule.Runtime): SecretKey {
      val keyGenerator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEY_STORE)

      val status = BiometricsManage.checkSupportBiometricsSync()
      val userAuthenticationRequired = when (status) {
        BiometricCheckResult.BIOMETRIC_ERROR_NONE_ENROLLED -> {
          askGoSettings(runtime)
          true
        }

        BiometricCheckResult.BIOMETRIC_SUCCESS -> true
        BiometricCheckResult.BIOMETRIC_STATUS_UNKNOWN -> false
        BiometricCheckResult.BIOMETRIC_ERROR_UNSUPPORTED -> false
        BiometricCheckResult.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false
        BiometricCheckResult.BIOMETRIC_ERROR_NO_HARDWARE -> false
        BiometricCheckResult.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> false
      }

      val params = KeyGenParameterSpec.Builder(
        alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      ).setBlockModes(BLOCK_MODES)
        // PKCS7 通常用于对称加密；RSA_OAEP 则通常用于非对称；RSA_PKCS1 比 RSA_OAEP 落后
        .setEncryptionPaddings(ENCRYPTION_PADDINGS).setRandomizedEncryptionRequired(true)
        // 要求进行生物识别认证
        .setUserAuthenticationRequired(userAuthenticationRequired).build()

      keyGenerator.init(params)
      return keyGenerator.generateKey()
    }

    private suspend fun askGoSettings(runtime: MicroModule.Runtime) {
      val biometricsActivity = BiometricsActivity.create(runtime)
      biometricsActivity.waitSupportOrThrow { Exception("fail to create secretKey in keychain") }
      biometricsActivity.finish()
    }
  }

  @Transient
  val cryptKey: SecretKey = SecretKeySpec(encoded, algorithm)

  @Transient
  private val keyInfo: KeyInfo = (SecretKeyFactory.getInstance(algorithm, ANDROID_KEY_STORE)
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

  /**
   * 生物识别
   */
  private suspend fun checkBiometrics(
    runtime: MicroModule.Runtime,
    remoteMmid: String,
    cipher: Cipher,
  ) {
    if (keyInfo.isUserAuthenticationRequired && keyInfo.userAuthenticationValidityDurationSeconds <= 0) {
      val biometricsActivity = BiometricsActivity.create(runtime)
      biometricsActivity.waitSupportOrThrow()
      biometricsActivity.authenticateWithClass3BiometricsDeferred(
        crypto = BiometricPrompt.CryptoObject(cipher),
        title = KeychainI18nResource.name.text,
        negativeButtonText = BiometricsI18nResource.cancel_button.text,
        subtitle = KeychainI18nResource.require_auth_subtitle.text,
        description = remoteMmid,
        confirmationRequired = true,
      )
    }
  }

  /**
   * 数据加密
   */
  override suspend fun encryptData(
    runtime: MicroModule.Runtime,
    remoteMmid: String,
    sourceData: ByteArray,
  ): ByteArray {
    val cipher: Cipher = Cipher.getInstance(TRANSFORMATION)
    cipher.init(Cipher.ENCRYPT_MODE, cryptKey)
    checkBiometrics(runtime, remoteMmid, cipher)

    val encryptedData = cipher.doFinal(sourceData)
    return cipher.iv + encryptedData
  }

  /**
   * 数据解密
   */
  override suspend fun decryptData(
    runtime: MicroModule.Runtime,
    remoteMmid: String,
    encryptedBytes: ByteArray,
  ): ByteArray {
    val cipher = Cipher.getInstance(TRANSFORMATION)
    val iv = encryptedBytes.sliceArray(0..<IV_SIZE_BYTES)
    val encryptedData = encryptedBytes.sliceArray(IV_SIZE_BYTES..<encryptedBytes.size)
    val spec = IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, cryptKey, spec)
    checkBiometrics(runtime, remoteMmid, cipher)

    return cipher.doFinal(encryptedData)
  }
}