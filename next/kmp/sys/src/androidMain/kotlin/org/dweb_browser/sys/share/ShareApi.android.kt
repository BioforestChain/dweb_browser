package org.dweb_browser.sys.share

import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.util.asStream
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.flow.asFlow
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.PromiseOut

actual suspend fun share(
  shareOptions: ShareOptions,
  multiPartData: MultiPartData?,
  shareNMM: MicroModule.Runtime,
): String {
  val files = mutableListOf<String>()
  multiPartData?.forEachPart { partData ->
    when (partData) {
      is PartData.FileItem -> {
        partData.provider.asFlow().collect {
          it.toInputStream()
        }
        partData.originalFileName?.also { filename ->
          val url = CacheFilePlugin.writeFile(
            filename, EFileType.Cache, partData.provider().toInputStream(), false
          )
          files.add(url)
        }
      }

      else -> {}
    }
    partData.dispose()
  }

  return share(shareOptions, files, shareNMM)
}

actual suspend fun share(
  shareOptions: ShareOptions,
  files: List<String>,
  shareNMM: MicroModule.Runtime,
): String {
  ShareController.controller.openActivity()
  ShareController.controller.waitActivityResultLauncherCreated()

  val result = PromiseOut<String>()
  // activity回调请求
  ShareController.controller.getShareData {
    result.resolve(it)
  }
  SharePlugin.share(ShareController.controller, shareOptions, files, result)
  val data = result.waitPromise()
  debugShare("share", "result => $data")
  ShareController.controller.activity?.finish()
  return data
}

