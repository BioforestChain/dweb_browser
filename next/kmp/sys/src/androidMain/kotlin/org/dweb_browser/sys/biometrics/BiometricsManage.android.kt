package org.dweb_browser.sys.biometrics

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.getOrDefault
import org.dweb_browser.helper.randomUUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


actual object BiometricsManage {

  actual suspend fun checkSupportBiometrics() = checkSupportBiometricsSync()
  fun checkSupportBiometricsSync(context: Context = getAppContextUnsafe()) =
    BiometricCheckResult.ALL_VALUES.getOrDefault(
      BiometricManager.from(context)
        .canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL),
      BiometricCheckResult.BIOMETRIC_STATUS_UNKNOWN
    ).let { result ->
      if (result == BiometricCheckResult.BIOMETRIC_SUCCESS) {
        /// 验证一下，用户是不是没有配置
        val missBiometric = runCatching {
          KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .init(
              KeyGenParameterSpec.Builder(
                randomUUID(), KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
              ).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                // 要求进行生物识别认证
                .setUserAuthenticationRequired(true)
                .build()
            )
          /// 如果这段代码跑成功了，说明有配置生物识别技术
          false
        }.getOrElse { e ->
          (e is java.security.InvalidAlgorithmParameterException && e.cause?.message == "At least one biometric must be enrolled to create keys requiring user authentication for every use")
        }
        if (missBiometric) {
          return@let BiometricCheckResult.BIOMETRIC_ERROR_NONE_ENROLLED
        }
      }
      return@let result
    }

  actual suspend fun biometricsAuthInRuntime(
    mmRuntime: MicroModule.Runtime,
    title: String?,
    subtitle: String?,
    description: String?,
  ): BiometricsResult {
    val biometricsActivity = BiometricsActivity.create(mmRuntime)
    biometricsActivity.waitSupportOrThrow()
    return biometricsActivity.startAuthenticate(
      title = title,
      subtitle = subtitle,
      description = description
    )
  }

  actual suspend fun biometricsAuthInGlobal(
    title: String?,
    subtitle: String?,
    description: String?,
  ): BiometricsResult {
    val biometricsActivity = BiometricsActivity.create(MicroModule::startAppActivity)
    biometricsActivity.waitSupportOrThrow()
    return biometricsActivity.startAuthenticate(
      title = title,
      subtitle = subtitle,
      description = description
    )
  }

  private suspend fun BiometricsActivity.startAuthenticate(
    title: String?,
    subtitle: String?,
    description: String?,
  ) = runCatching {
    authenticateWithClass3BiometricsDeferred(
      crypto = null,
      title = title ?: BiometricsI18nResource.default_title.text,
      negativeButtonText = BiometricsI18nResource.cancel_button.text,
      subtitle = subtitle ?: BiometricsI18nResource.default_subtitle.text,
      description = description,
      confirmationRequired = true,
    )
    BiometricsResult(true, "")
  }.getOrElse {
    BiometricsResult(false, it.message ?: "authenticateWithClass3Biometrics error")
  }

  private fun generateSecretKey(mmid: MMID): SecretKey {
    debugBiometrics("generateSecretKey", mmid)
    val keyGenParameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
      mmid, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    ).setBlockModes(KeyProperties.BLOCK_MODE_CBC)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
      .setUserAuthenticationRequired(true)
      // Invalidate the keys if the user has registered a new biometric
      // credential, such as a new fingerprint. Can call this method only
      // on Android 7.0 (API level 24) or higher. The variable
      // "invalidatedByBiometricEnrollment" is true by default.
      .setInvalidatedByBiometricEnrollment(true).build()
    val keyGenerator = KeyGenerator.getInstance(
      KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
    )
    keyGenerator.init(keyGenParameterSpec)
    return keyGenerator.generateKey()
  }

  fun encryptData(plaintext: ByteArray, cipher: Cipher) = cipher.doFinal(plaintext)
  fun decryptData(ciphertext: ByteArray, cipher: Cipher) = cipher.doFinal(ciphertext)
}