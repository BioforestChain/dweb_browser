package org.dweb_browser.sys.Scanning

import io.ktor.http.HttpMethod
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.toJsonElement

fun debugScanning(tag: String, msg: Any? = "", err: Throwable? = null) =
    printDebug("Scanning", tag, msg, err)

class ScanningNMM: NativeMicroModule("barcode-scanning.sys.dweb", "Barcode Scanning") {
    init {
        categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Utilities);
        short_name = "Scanning"
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        routes(
            // 处理二维码图像
            "/process" bind HttpMethod.Post to defineJsonResponse {
                debugScanning(
                    "process",
                    " ${request.queryOrNull("rotation")?.toInt() ?: 0} ${request.body.contentLength}"
                )

                val imgBitArray = request.body.toPureBinary() ?: return@defineJsonResponse emptyList<String>().toJsonElement()
                val result = getScanningController().recognize(imgBitArray, request.queryOrNull("rotation")?.toInt() ?: 0)
                debugScanning("process", "result=> $result")
                return@defineJsonResponse result.toJsonElement()
            },

            // 停止处理
            "/stop" bind HttpMethod.Get to defineBooleanResponse {
                getScanningController().stopScan()
                return@defineBooleanResponse true
            },

            "/open" bind HttpMethod.Get to defineStringResponse {
                val controller = getScanningController()
                if (controller.cameraPermission()) {
                    getScanningController().startScan()
                } else {
                    // TODO: 多语言
                    "permission denied"
                }
            }
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }

}