package org.dweb_browser.sys.biometrics

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication

actual object BiometricsApi {

  private val context = LAContext()

  actual suspend fun isSupportBiometrics(
    biometricsData: BiometricsData, biometricsNMM: BiometricsNMM
  ): Boolean {
    return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)
  }

  actual suspend fun biometricsResultContent(biometricsNMM: BiometricsNMM): BiometricsResult =
    suspendCancellableCoroutine { continuation ->
      context.evaluatePolicy(
        LAPolicyDeviceOwnerAuthentication, "Access requires authentication"
      ) { success, error ->
        var message: String = ""
        if (success) {

        } else {
          message = "biometrics has been destroyed"
        }
        val result = BiometricsResult(success, message)
        continuation.resume(
          value = result,
          onCancellation = {}
        )
      }
    }
}