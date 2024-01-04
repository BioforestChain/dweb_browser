package org.dweb_browser.sys.biometrics

import android.content.Intent
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toBase64
import org.dweb_browser.helper.withMainContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

actual object BiometricsManage {

  actual suspend fun isSupportBiometrics(
    biometricsData: BiometricsData, biometricsNMM: BiometricsNMM
  ): Boolean {
    return when (val info = BiometricManager.from(getAppContext())
      .canAuthenticate(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)) {
      BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
        debugBiometrics("canAuthenticate-true", info)
        true
      }

      else -> {
        debugBiometrics("canAuthenticate-false", info)
        false
      }
    }
  }

  actual suspend fun biometricsResultContent(
    biometricsNMM: BiometricsNMM,
    title: String?,
    subtitle: String?,
    input: ByteArray?,
    mode: InputMode
  ): BiometricsResult {
    val resultDeferred = CompletableDeferred<BiometricsResult>()

    val biometricsActivity = CompletableDeferred<BiometricsActivity>().run {
      val uid = randomUUID()
      BiometricsActivity.creates[uid] = this
      invokeOnCompletion { BiometricsActivity.creates.remove(uid) }
      biometricsNMM.startAppActivity(BiometricsActivity::class.java) { intent ->
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        intent.putExtras(
          Bundle().apply {
            putString("uid", uid)
          }
        )
      }
      await()
    }

    resultDeferred.invokeOnCompletion {
      biometricsActivity.finish()
    }

    val executor = ContextCompat.getMainExecutor(biometricsActivity)
    val biometricPrompt = BiometricPrompt(biometricsActivity, executor,
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(
          errorCode: Int,
          errString: CharSequence
        ) {
          super.onAuthenticationError(errorCode, errString)
          resultDeferred.complete(BiometricsResult(false, errString.toString()))
        }

        override fun onAuthenticationSucceeded(
          result: BiometricPrompt.AuthenticationResult
        ) {
          super.onAuthenticationSucceeded(result)
          debugBiometrics(
            "onAuthenticationSucceeded",
            result.cryptoObject ?: "<no cryptoObject>"
          )
          try {
            val cipherResult = if (input != null) {
              result.cryptoObject?.cipher?.let { cipher ->
                encryptData(input, cipher)
              }
            } else null
            if (cipherResult == null) {
              resultDeferred.complete(BiometricsResult(true, ""))
            } else {
              resultDeferred.complete(BiometricsResult(true, cipherResult.toBase64(), "base64"))
            }
          } catch (e: Throwable) {
            resultDeferred.complete(BiometricsResult(true, ""))
          }
        }

        override fun onAuthenticationFailed() {
          super.onAuthenticationFailed()
          resultDeferred.complete(
            BiometricsResult(
              false,
              BiometricsI18nResource.authentication_failed.text
            )
          )
        }
      })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title ?: BiometricsI18nResource.default_title.text)
      .setSubtitle(subtitle ?: BiometricsI18nResource.default_subtitle.text)
      .setDescription(biometricsNMM.mmid)
      .setNegativeButtonText(BiometricsI18nResource.cancel_button.text)
      .setConfirmationRequired(true)
      .build()

    val crypto = when (mode) {
      InputMode.None -> {
        null
      }

      InputMode.ENCRYPT, InputMode.DECRYPT -> {
        val secretKey = getSecretKey(biometricsNMM.mmid)
        val cipher = getCipher()

        cipher.init(
          if (mode == InputMode.DECRYPT)
            Cipher.DECRYPT_MODE else Cipher.ENCRYPT_MODE, secretKey
        )
        BiometricPrompt.CryptoObject(cipher)
      }
    }
    withMainContext {
      if (crypto == null) {
        biometricPrompt.authenticate(promptInfo)
      } else {
        biometricPrompt.authenticate(promptInfo, crypto)
      }
    }

    return resultDeferred.await()
  }

  private fun getCipher(): Cipher {
    return Cipher.getInstance(
      KeyProperties.KEY_ALGORITHM_AES + "/"
          + KeyProperties.BLOCK_MODE_CBC + "/"
          + KeyProperties.ENCRYPTION_PADDING_PKCS7
    )
  }

  private fun getSecretKey(mmid: MMID): SecretKey {
    debugBiometrics("getSecretKey", mmid)
    val keyStore = KeyStore.getInstance("AndroidKeyStore")

    // Before the keystore can be accessed, it must be loaded.
    keyStore.load(null)

    return keyStore.getKey(mmid, null) as SecretKey? ?: generateSecretKey(mmid)
  }

  private fun generateSecretKey(mmid: MMID): SecretKey {
    debugBiometrics("generateSecretKey", mmid)
    val keyGenParameterSpec: KeyGenParameterSpec = KeyGenParameterSpec.Builder(
      mmid,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
      .setUserAuthenticationRequired(true)
      // Invalidate the keys if the user has registered a new biometric
      // credential, such as a new fingerprint. Can call this method only
      // on Android 7.0 (API level 24) or higher. The variable
      // "invalidatedByBiometricEnrollment" is true by default.
      .setInvalidatedByBiometricEnrollment(true)
      .build()
    val keyGenerator = KeyGenerator.getInstance(
      KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
    )
    keyGenerator.init(keyGenParameterSpec)
    return keyGenerator.generateKey()
  }

  fun encryptData(plaintext: ByteArray, cipher: Cipher) = cipher.doFinal(plaintext)
  fun decryptData(ciphertext: ByteArray, cipher: Cipher) = cipher.doFinal(ciphertext)
}