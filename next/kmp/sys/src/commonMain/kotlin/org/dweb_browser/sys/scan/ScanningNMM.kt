package org.dweb_browser.sys.scan

import io.ktor.http.HttpMethod
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.PureBinaryFrame
import org.dweb_browser.core.http.PureTextFrame
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.toJsonElement

val debugScanning = Debugger("Scanning")

class ScanningNMM : NativeMicroModule("barcode-scanning.sys.dweb", "Barcode Scanning") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Utilities);
    short_name = "Scanning"
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val scanningManager = ScanningManager()
    routes(
      "/process" byChannel { ctx ->
        println("QAQ process start")
        var rotation = 0;
        for (frame in ctx) {
          println("QAQ process frame=>${frame}")
          try {
            when (frame) {
              is PureTextFrame -> rotation = frame.data.toFloatOrNull()?.toInt() ?: 0;
              is PureBinaryFrame -> {
                println("QAQ process size=>${frame.data.size}")
                val result = scanningManager.recognize(
                  frame.data,
                  rotation
                );
                // 不论 result 是否为空数组，都进行响应
                println("QAQ process result=>${result}")
                ctx.sendJson(result)
              }
            }
          } catch (e: Throwable) {
            println("QAQ process err!")
            e.printStackTrace()
          }

        }
        println("QAQ process end")
      },
      // 处理二维码图像
      "/process" bind HttpMethod.Post by defineJsonResponse {
        val rotation = request.queryOrNull("rotation")?.toFloatOrNull()?.toInt() ?: 0
        debugScanning(
          "/process",
        ) {
          "rotation:$rotation imageSize:${request.body.contentLength}"
        }

        val imgBitArray = request.body.toPureBinary()
        val result = scanningManager.recognize(
          imgBitArray,
          rotation
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