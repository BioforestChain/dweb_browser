package info.bagen.dwebbrowser.microService.sys.barcodeScanning

import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.core.BootstrapContext
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.MICRO_MODULE_CATEGORY
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes

fun debugScanning(tag: String, msg: Any? = "", err: Throwable? = null) =
  printdebugln("Scanning", tag, msg, err)

class ScanningNMM : NativeMicroModule("barcode-scanning.sys.dweb","Barcode Scanning") {

    override val categories = mutableListOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Utilities);

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val query_rotationDegrees = Query.int().defaulted("rotation", 0)

        apiRouting = routes(
            // 处理二维码图像
            "/process" bind Method.POST to defineHandler { request, ipc ->
                debugScanning(
                    "process",
                    " ${query_rotationDegrees(request)} ${request.body.length}"
                )
                val image = InputImage.fromBitmap(
                    request.body.payload.let { buff ->
                        val byteArray = buff.array()
                        BitmapFactory.decodeByteArray(
                            byteArray,
                            0,
                            byteArray.size
                        )
                    },
                    query_rotationDegrees(request)
                )
                val result = mutableListOf<String>()
                process(image).forEach {
                    result.add(String(it.data))
                }
                debugScanning(
                    "process",
                    "result=> $result"
                )
                return@defineHandler result
            },
            // 停止处理
            "/stop" bind Method.GET to defineHandler { request ->
                stop()
                return@defineHandler true
            }
        )
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