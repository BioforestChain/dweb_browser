package org.dweb_browser.sys.biometrics

import android.content.Intent
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.toJsonElement

actual object BiometricsApi {

  actual suspend fun isSupportBiometrics(
    biometricsData: BiometricsData, biometricsNMM: BiometricsNMM
  ): Boolean {
    openActivity(biometricsNMM)
    val activity = BiometricsController.biometricsController.waitActivityCreated()
    return activity.chuck().also { activity.finish() }
  }

  actual suspend fun biometricsResultContent(biometricsNMM: BiometricsNMM): BiometricsResult {
    BiometricsActivity.biometrics_promise_out = PromiseOut()
    openActivity(biometricsNMM)
    val activity = BiometricsController.biometricsController.waitActivityCreated()
    activity.biometrics()
    return BiometricsActivity.biometrics_promise_out.waitPromise()
  }

  private fun openActivity(biometricsNMM: BiometricsNMM, data: BiometricsData? = null) {
    val context = biometricsNMM.getAppContext()
    val intent = Intent(context, BiometricsActivity::class.java)
    data?.let { intent.putExtra("data", data.toJsonElement().toString()) }
    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
  }
}