package org.dweb_browser.sys.media

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.ipc.helper.IpcMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.platform.MultiPartFile

val debugMedia = Debugger("Media")

class MediaNMM : NativeMicroModule("media.sys.dweb", "system media") {
  init {
    short_name = "Media";
    categories =
      listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Device_Management_Service)
    /*dweb_permissions = listOf(
      DwebPermission(
        pid = "$mmid/savePictures",
        routes = listOf("file://$mmid/savePictures"),
        title = "存储到相册",
        permissionType = emptyList()
      )
    )*/
  }

  @OptIn(ExperimentalSerializationApi::class)
  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      /** 保存图片到相册*/
      "/savePictures" bind IpcMethod.POST by defineEmptyResponse {
        val byteArray = request.body.toPureBinary()
        val saveLocation = request.queryOrNull("saveLocation")
        debugMedia("/savePictures", "saveLocation=$saveLocation byteArray = ${byteArray.size}")

        val files = Cbor.decodeFromByteArray<List<MultiPartFile>>(byteArray)
        // 目前只支持保存一个文件
        savePictures(saveLocation, files)

      }
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}
