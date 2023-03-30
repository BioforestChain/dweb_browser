package info.bagen.rust.plaoc.microService.sys.plugin.camera

import android.graphics.Bitmap
import info.bagen.rust.plaoc.microService.core.AndroidNativeMicroModule
import info.bagen.rust.plaoc.microService.core.BootstrapContext
import info.bagen.rust.plaoc.microService.core.NativeMicroModule
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.printdebugln
import info.bagen.rust.plaoc.microService.helper.runBlockingCatching
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.io.ByteArrayOutputStream

inline fun debugCameraNMM(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("Camera", tag, msg, err)

class CameraNMM() : AndroidNativeMicroModule("camera.sys.dweb") {

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val query_source = Query.string().defaulted("source", "PHOTOS")
        val query_quality = Query.int().defaulted("quality", 50)

        apiRouting = routes(
            // 打开相册
            "/getPhoto" bind Method.GET to defineHandler { request, ipc ->
                val source = CameraSource.valueOf(query_source(request))
                val quality = query_quality(request);
                debugCameraNMM("getPhoto", "uri: ${request.uri},remoteId: ${ipc.remote.mmid}")
                val cameraPlugin = CameraPlugin(getActivity(ipc.remote.mmid))
                val bitmap = PromiseOut<Bitmap>()
                cameraPlugin.getPhoto(CameraSettings(source = source)) {
                    debugCameraNMM("getPhoto error", "result => $it")
                    bitmap.reject(Exception(it))
                }
                getCurrentWebViewController(ipc.remote.mmid)?.getPhotoData { bit ->
                    debugCameraNMM("getPhotoData", "result => $bit")
                    if (bit == null) {
                        return@getPhotoData bitmap.reject(Exception("The user did not select an image."))
                    }
                    bitmap.resolve(bit)
                }
                getCurrentWebViewController(ipc.remote.mmid)?.getCameraData {
                    if(it == null) {
                        return@getCameraData bitmap.reject(Exception("The user did not take a photo."))
                    }
                    bitmap.resolve(it)
                    debugCameraNMM("getCameraData", "result => $it")
                }

                val result = bitmap.waitPromise()
                val bao = ByteArrayOutputStream()
                result.compress(Bitmap.CompressFormat.JPEG, quality, bao);
                return@defineHandler bao.toByteArray()
            },
        )
    }


    override suspend fun _shutdown() {
    }
}