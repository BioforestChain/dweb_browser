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

  inner class BiometricsRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {

    override suspend fun _bootstrap() {
      routes(
        /** 检查识别支持生物识别*/
        "/check" bind PureMethod.GET by defineNumberResponse {
          val type = request.queryOrNull("type") ?: ""
          val biometricsData = request.queryAs<BiometricsData>()
          debugBiometrics("check", "type=$type, data=$biometricsData")
          BiometricsManage.checkSupportBiometrics().value
        },
        /** 生物识别*/
        "/biometrics" bind PureMethod.GET by defineJsonResponse {
          val title = request.queryOrNull("title")
          val subtitle = request.queryOrNull("subtitle")
          val biometricsResult =
            BiometricsManage.biometricsAuthInRuntime(
              this@BiometricsRuntime,
              title,
              subtitle,
              ipc.remote.mmid,
            )
          debugBiometrics("biometrics", biometricsResult.toJsonElement())
          return@defineJsonResponse biometricsResult.toJsonElement()
        },
        "/biometrics" bind PureMethod.POST by defineJsonResponse {
          val title = request.queryOrNull("title")
          val subtitle = request.queryOrNull("subtitle")

          val biometricsResult =
            BiometricsManage.biometricsAuthInRuntime(
              this@BiometricsRuntime,
              title,
              subtitle,
              ipc.remote.mmid,
            )
          debugBiometrics("biometrics", biometricsResult.toJsonElement())
          return@defineJsonResponse biometricsResult.toJsonElement()
        }
      ).cors()
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) =
    BiometricsRuntime(bootstrapContext)
}