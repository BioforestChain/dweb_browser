package info.bagen.dwebbrowser.microService.sys.barcodeScanning

import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import info.bagen.dwebbrowser.microService.sys.permission.EPermission
import io.ktor.http.HttpMethod
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.PromiseOut
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
    routes(
      // 处理二维码图像
      "/process" bind HttpMethod.Post to defineJsonResponse {
        debugScanning(
          "process",
          " ${request.queryOrNull("rotation")?.toInt() ?: 0} ${request.body.contentLength}"
        )
        val image = InputImage.fromBitmap(
          request.body.toPureBinary().let { byteArray ->
            BitmapFactory.decodeByteArray(
              byteArray,
              0,
              byteArray.size
            )
          },
          request.queryOrNull("rotation")?.toInt() ?: 0
        )
        val result = mutableListOf<String>()
        process(image).forEach {
          result.add(String(it.data))
        }
        debugScanning("process", "result=> $result")
        return@defineJsonResponse result.toJsonElement()
      },
      // 停止处理
      "/stop" bind HttpMethod.Get to defineBooleanResponse {
        stop()
        return@defineBooleanResponse true
      },
      "/open" bind HttpMethod.Get to defineStringResponse {
        val grant =
          nativeFetch("file://permission.sys.dweb/query?permission=${EPermission.PERMISSION_CAMERA}").boolean()
        if (grant) {
          openScanningActivity()
        } else {
          "permission denied"
        }
      }
    )
  }

  private suspend fun openScanningActivity(): String {
    startAppActivity(ScanningActivity::class.java) {
      ScanningController.controller.scanData = null
    }
    return ScanningController.controller.waitScanResult()
  }

  override suspend fun _shutdown() {
  }

  class BarcodeResult(val data: ByteArray, val boundingBox: Rect, val cornerPoints: List<Point>)

  private suspend fun process(image: InputImage): List<BarcodeResult> {
    val task = PromiseOut<List<BarcodeResult>>()
    BarcodeScanning.getClient().process(image)
      .addOnSuccessListener { barcodes ->
        task.resolve(barcodes.map {
          BarcodeResult(
            it.rawBytes!!,
            it.boundingBox!!,
            it.cornerPoints!!.toList()
          )
        })
      }
      .addOnFailureListener { err ->
        task.reject(err)
      }
    return task.waitPromise()
  }

  private fun stop() {
    return BarcodeScanning.getClient().close()
  }

}