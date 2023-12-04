package org.dweb_browser.sys.scan

import io.ktor.http.HttpMethod
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.toJsonElement

fun debugScanning(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Scanning", tag, msg, err)

class ScanningNMM : NativeMicroModule("barcode-scanning.sys.dweb", "Barcode Scanning") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Utilities);
    short_name = "Scanning"
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val scanningManager = ScanningManager()
    routes(
      // 处理二维码图像
      "/process" bind HttpMethod.Post by defineJsonResponse {
        debugScanning(
          "process",
          " ${request.queryOrNull("rotation")?.toInt() ?: 0} ${request.body.contentLength}"
        )

        val imgBitArray = request.body.toPureBinary()
        val result = scanningManager.recognize(
          imgBitArray,
          request.queryOrNull("rotation")?.toInt() ?: 0
        )
        debugScanning("process", "result=> $result")
        return@defineJsonResponse result.toJsonElement()
      },

      // 停止处理
      "/stop" bind HttpMethod.Get by defineBooleanResponse {
        scanningManager.stop()
        return@defineBooleanResponse true
      },
    )
  }

  override suspend fun _shutdown() {

  }

}