package org.dweb_browser.sys.biometrics

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.Ipc
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication

actual object BiometricsApi {

    private val context = LAContext()

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun isSupportBiometrics(ipc: Ipc, request: PureRequest): Boolean {
        val isSupport = context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)
        return isSupport
    }

    actual suspend fun biometricsResultContent(ipc: Ipc): BiometricsResult = suspendCancellableCoroutine { continuation ->
        context.evaluatePolicy(LAPolicyDeviceOwnerAuthentication,"Access requires authentication") { success,error ->
            var message: String = ""
            if (success) {

            } else {
                message = "biometrics has been destroyed"
            }
            val result = BiometricsResult(success,message)
            continuation.resume(
                value = result,
                onCancellation = {}
            )
        }
    }
}