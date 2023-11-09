package org.dweb_browser.sys.biometrics

import io.ktor.http.HttpMethod
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.toJsonElement

fun debugBiometrics(tag: String, msg: Any? = "", err: Throwable? = null) =
    printDebug("biometrics", tag, msg, err)

class BiometricsNMM : NativeMicroModule("biometrics.sys.dweb", "biometrics") {

    init {
        categories =
            listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Machine_Learning_Service);
    }

//  val queryType = Query.string().defaulted("type", "")
//  val queryBiometricsData = Query.composite { spec ->
//    BiometricsData(
//      title = string().optional("title")(spec),
//      subtitle = string().optional("subtitle")(spec),
//      description = string().optional("description")(spec),
//      useFallback = boolean().optional("useFallback")(spec),
//      negativeButtonText = string().optional("negativeButtonText")(spec),
//    )
//  }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        routes(/** 检查识别支持生物识别*/
            "/check" bind HttpMethod.Get to defineBooleanResponse {
                val type = request.queryOrNull("type") ?: ""
                debugBiometrics("check", type)

                val result = BiometricsApi.isSupportBiometrics(ipc,request)
                return@defineBooleanResponse result
            },
            /** 生物识别*/
            "/biometrics" bind HttpMethod.Get to defineJsonResponse {

                val biometricsResult = BiometricsApi.biometricsResultContent(ipc)
                print(biometricsResult.toJsonElement())
                return@defineJsonResponse biometricsResult.toJsonElement()
            }).cors()
    }

    override suspend fun _shutdown() {
    }
}