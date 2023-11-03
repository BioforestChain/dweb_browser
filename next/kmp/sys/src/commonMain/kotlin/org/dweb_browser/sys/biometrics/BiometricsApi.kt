package org.dweb_browser.sys.biometrics

import kotlinx.serialization.Serializable
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.Ipc

@Serializable
data class BiometricsResult(val success: Boolean, val message: String)

expect object BiometricsApi {

    suspend fun isSupportBiometrics(ipc: Ipc, request: PureRequest): Boolean

    suspend fun biometricsResultContent(ipc: Ipc): BiometricsResult
}

