package info.bagen.rust.plaoc.microService.sys.plugin.barcode

import android.graphics.BitmapFactory
import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.sys.plugin.camera.FlashLightUtils
import io.ktor.util.*
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugScanning(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("Scanning", tag, msg, err)

class ScanningNMM() : NativeMicroModule("scanning.sys.dweb") {

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val query_rotationDegrees = Query.int().defaulted("rotation", 0)

        apiRouting = routes(
            // 处理二维码图像
            "/process" bind Method.POST to defineHandler { request, ipc ->
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
                return@defineHandler process(image)
            },
            // 停止处理
            "/stop" bind Method.GET to defineHandler { request ->
                stop()
                return@defineHandler true
            },
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