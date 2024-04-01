package org.dweb_browser.sys.scan

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.pure.http.PureBinaryFrame
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame

val debugScanning = Debugger("Scanning")

class ScanningNMM : NativeMicroModule.NativeRuntime("barcode-scanning.sys.dweb", "Barcode Scanning") {
  init {
    categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Utilities);
    short_name = "Scanning"
  }

  private val scanningManager = ScanningManager()

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/process" byChannel { ctx ->
        val time = datetimeNow()
        println("lin.huang => process byChannel enter=>$time")
        var rotation = 0
        for (frame in ctx) {
          when (frame) {
            is PureTextFrame -> {
              debugScanning("process=>byChannel", "PureTextFrame($time)")
              rotation = frame.data.toIntOrNull() ?: 0
            }

            is PureBinaryFrame -> {
              debugScanning("process=>byChannel", "PureBinaryFrame($time) $rotation")
              val result = try {
                scanningManager.recognize(frame.data, rotation)
              } catch (e: Throwable) {
                debugScanning("process=>byChannel", null, e)
                emptyList()
              }
              debugScanning("process=>byChannel", result.joinToString(", ") { it.data })
              // 不论 result 是否为空数组，都进行响应
              ctx.sendJson(result)
            }

            else -> {
              ctx.getChannel().close()
            }
          }
        }
      },
      // 处理二维码图像
      "/process" bind PureMethod.POST by defineJsonResponse {
        val rotation = request.queryOrNull("rotation")?.toIntOrNull() ?: 0
        debugScanning("process=>POST", "rotation=$rotation, ${request.body.contentLength}")

        val imgBitArray = request.body.toPureBinary()
        val result = try {
          scanningManager.recognize(imgBitArray, rotation)
        } catch (e: Throwable) {
          debugScanning("process=>POST", null, e)
          emptyList()
        }
        debugScanning("process=>POST", result.joinToString(", ") { it.data })
        return@defineJsonResponse result.toJsonElement()
      },

      // 停止处理
      "/stop" bind PureMethod.GET by defineBooleanResponse {
        scanningManager.stop()
        return@defineBooleanResponse true
      },
    ).cors()
  }

  override suspend fun _shutdown() {
  }
}