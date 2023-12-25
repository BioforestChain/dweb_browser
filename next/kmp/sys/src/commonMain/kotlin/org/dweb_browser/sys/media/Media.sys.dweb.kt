package org.dweb_browser.sys.media


import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.DwebResult
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.toJsonElement

fun debugMedia(tag: String, msg: Any? = "", err: Throwable? = null) =
  printDebug("Media", tag, msg, err)

class MediaNMM : NativeMicroModule("media.sys.dweb", "system media") {
  init {
    short_name = "Media";
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service);
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 保存图片到相册*/
      "/savePictures" bind IpcMethod.POST by defineJsonResponse {
        debugMedia("/savePictures")
        val byteArray = request.body.toPureBinary()
        println("byteArray = ${byteArray.size}")

//        val files = Cbor.decodeFromByteArray<List<MultiPartFile>>(byteArray)
//        val fileList = mutableListOf<String>()

        return@defineJsonResponse DwebResult(true).toJsonElement()
      }
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }


}
