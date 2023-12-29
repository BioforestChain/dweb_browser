package org.dweb_browser.sys.biometrics

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.queryAs

val debugBiometrics = Debugger("biometrics")

class BiometricsNMM : NativeMicroModule("biometrics.sys.dweb", "biometrics") {

  init {
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Machine_Learning_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(/** 检查识别支持生物识别*/
      "/check" bind PureMethod.GET by defineBooleanResponse {
        val type = request.queryOrNull("type") ?: ""
        val biometricsData = request.queryAs<BiometricsData>()
        debugBiometrics("check", "type=$type, data=$biometricsData")
        return@defineBooleanResponse BiometricsApi.isSupportBiometrics(
          biometricsData = biometricsData, biometricsNMM = this@BiometricsNMM
        )
      },
      /** 生物识别*/
      "/biometrics" bind PureMethod.GET by defineJsonResponse {
        val biometricsResult = BiometricsApi.biometricsResultContent(this@BiometricsNMM)
        debugBiometrics("biometrics", biometricsResult.toJsonElement())
        return@defineJsonResponse biometricsResult.toJsonElement()
      }).cors()
  }

  override suspend fun _shutdown() {
  }
}