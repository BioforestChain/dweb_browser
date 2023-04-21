package info.bagen.dwebbrowser.microService.sys.plugin.camera

import info.bagen.dwebbrowser.microService.core.BootstrapContext
import info.bagen.dwebbrowser.microService.core.NativeMicroModule
import info.bagen.dwebbrowser.microService.helper.printdebugln
import org.http4k.core.Method
import org.http4k.lens.Query
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.routing.bind
import org.http4k.routing.routes

inline fun debugCameraNMM(tag: String, msg: Any? = "", err: Throwable? = null) =
    printdebugln("Camera", tag, msg, err)

class CameraNMM() : NativeMicroModule("camera.sys.dweb") {
    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        val query_source = Query.string().defaulted("source", "PHOTOS")
        val query_quality = Query.int().defaulted("quality", 50)

        apiRouting = routes(
            // 打开相册
            "/getPhoto" bind Method.GET to defineHandler { request, ipc ->
                // TODO 使用原生接口替代
                return@defineHandler "使用原生接口替代"
//                val source = CameraSource.valueOf(query_source(request))
//                val quality = query_quality(request);
//                debugCameraNMM("getPhoto", "uri: ${request.uri},remoteId: ${ipc.remote.mmid}")
//                val cameraPlugin = CameraPlugin(getActivity(ipc.remote.mmid))
//                val bitmap = PromiseOut<Bitmap>()
//                cameraPlugin.getPhoto(CameraSettings(source = source)) {
//                    debugCameraNMM("getPhoto error", "result => $it")
//                    bitmap.reject(Exception(it))
//                }
//                getCurrentWebViewController(ipc.remote.mmid)?.getPhotoData { bit ->
//                    debugCameraNMM("getPhotoData", "result => $bit")
//                    if (bit == null) {
//                        return@getPhotoData bitmap.reject(Exception("The user did not select an image."))
//                    }
//                    bitmap.resolve(bit)
//                }
//                getCurrentWebViewController(ipc.remote.mmid)?.getCameraData {
//                    if(it == null) {
//                        return@getCameraData bitmap.reject(Exception("The user did not take a photo."))
//                    }
//                    bitmap.resolve(it)
//                    debugCameraNMM("getCameraData", "result => $it")
//                }
//
//                val result = bitmap.waitPromise()
//                val bao = ByteArrayOutputStream()
//                result.compress(Bitmap.CompressFormat.JPEG, quality, bao);
//                return@defineHandler bao.toByteArray()
            },
        )
    }

    // 选中照片返回数据
//    if (requestCode == REQUEST_IMAGE_CAPTURE) {
//        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
//            val imageData = data.data?.toBitmap(contentResolver)
//            GlobalScope.launch(ioAsyncExceptionHandler) {
//                controller?.getPhotoSignal?.emit(imageData)
//                debugCameraNMM("REQUEST_IMAGE_CAPTURE", imageData)
//            }
//        } else {
//            // 没有选中图片直接返回
//            GlobalScope.launch(ioAsyncExceptionHandler) {
//                controller?.getPhotoSignal?.emit(null)
//                debugCameraNMM("REQUEST_IMAGE_CAPTURE", "没有选中图片直接返回")
//            }
//        }
//
//    }
//    // 拍照返回数据处理
//    if (requestCode == REQUEST_CAMERA_IMAGE) {
//        if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
//            val imageBitmap = data.extras?.get("data") as Bitmap
//            GlobalScope.launch(ioAsyncExceptionHandler) {
//                controller?.getCameraSignal?.emit(imageBitmap)
//                debugCameraNMM("REQUEST_CAMERA_IMAGE", imageBitmap)
//            }
//        } else {
//            // 没有拍照直接返回
//            GlobalScope.launch(ioAsyncExceptionHandler) {
//                controller?.getCameraSignal?.emit(null)
//                debugCameraNMM("REQUEST_CAMERA_IMAGE", "没有拍照直接返回")
//            }
//        }
//    }

//    val getCameraSignal = Signal<Bitmap?>()
//    val getPhotoSignal = Signal<Bitmap?>()
//    fun getCameraData(cb: Callback<Bitmap?>) = getCameraSignal.listen(cb)
//    fun getPhotoData(cb: Callback<Bitmap?>) = getPhotoSignal.listen(cb)


    override suspend fun _shutdown() {
    }
}