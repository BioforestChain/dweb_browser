package info.bagen.dwebbrowser.microService.sys.plugin.barcode

import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.PromiseOut
import info.bagen.dwebbrowser.microService.helper.printdebugln
import io.ktor.util.*
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugScanning(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("Scanning", tag, msg, err)

class ScanningNMM() : NativeMicroModule("barcode-scanning.sys.dweb") {

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
                    request.body.payload.moveToByteArray().let { byteArray ->
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
                debugScanning("process", "result=> $result")
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
                        it.rawBytes,
                        it.boundingBox,
                        it.cornerPoints.toList()
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