package org.dweb_browser.sys.biometrics

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CompletableDeferred
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication

actual object BiometricsManage {

  @OptIn(ExperimentalForeignApi::class)
  actual suspend fun isSupportBiometrics(
    biometricsData: BiometricsData, biometricsNMM: BiometricsNMM
  ): Boolean {
    return LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)
  }

  actual suspend fun biometricsResultContent(
    biometricsNMM: BiometricsNMM,
    title: String?,
    subtitle: String?,
    input: ByteArray?,
    mode: InputMode
  ): BiometricsResult {
    val result = CompletableDeferred<BiometricsResult>()
    val safeTitle = title ?: BiometricsI18nResource.default_title.text
    val safeSubtitle = subtitle ?: BiometricsI18nResource.default_subtitle.text
    val description = biometricsNMM.mmid
    LAContext().evaluatePolicy(
      LAPolicyDeviceOwnerAuthentication, "$safeTitle\n $safeSubtitle\n $description"
    ) { success, error ->
      var message = ""
      if (!success) {
        message = error?.localizedDescription ?: BiometricsI18nResource.authentication_failed.text
      }

      result.complete(BiometricsResult(success, message))
    }
    return result.await()
  }
}