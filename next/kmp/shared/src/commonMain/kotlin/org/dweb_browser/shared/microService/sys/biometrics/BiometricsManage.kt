package org.dweb_browser.shared.microService.sys.biometrics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.Ipc


@Serializable
data class BiometricsResult(val success: Boolean, val message: String)

expect object BiometricsManage {

    suspend fun isSupportBiometrics(ipc: Ipc, request: PureRequest): Boolean

    suspend fun biometricsResultContent(ipc: Ipc): BiometricsResult
}
