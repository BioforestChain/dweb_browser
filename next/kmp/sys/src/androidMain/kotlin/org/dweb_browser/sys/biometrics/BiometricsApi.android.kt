package org.dweb_browser.sys.biometrics

import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.Ipc

actual object BiometricsApi {

    actual suspend fun isSupportBiometrics(ipc: Ipc, request: PureRequest): Boolean {
        return false
    }

    actual suspend fun biometricsResultContent(ipc: Ipc): BiometricsResult {
        return BiometricsResult(false,"need android code")
    }


}