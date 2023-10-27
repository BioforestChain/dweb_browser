package org.dweb_browser.shared.microService.sys.biometrics

import android.content.Intent
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.NativeMicroModule
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonElement
import org.dweb_browser.core.http.PureRequest
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.shared.microService.sys.biometrics.BiometricsController.Companion.biometricsController
import org.dweb_browser.shared.microService.sys.biometrics.BiometricsActivity.Companion.biometrics_promise_out


actual object BiometricsManage {

    actual suspend fun isSupportBiometrics(ipc: Ipc, request: PureRequest): Boolean {
        openActivity(ipc.remote.mmid, request.queryAs<BiometricsData>())
        val context = biometricsController.waitActivityCreated()
        val result = context.chuck()
        context.finish()
        return result
    }

    actual suspend fun biometricsResultContent(ipc: Ipc): BiometricsResult {
        biometrics_promise_out = PromiseOut()
        debugBiometrics("fingerprint", ipc.remote.mmid)
        openActivity(ipc.remote.mmid)
        val context = biometricsController.waitActivityCreated()
        context.biometrics()

        return biometrics_promise_out.waitPromise()
    }

    private fun openActivity(remoteMMID: MMID) {
        val context = NativeMicroModule.getAppContext()
        val intent = Intent(context, BiometricsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openActivity(remoteMMID: MMID, data: BiometricsData) {
        val context = NativeMicroModule.getAppContext()
        val intent = Intent(context, BiometricsActivity::class.java)
        intent.putExtra("data", data)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}