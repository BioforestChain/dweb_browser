package org.dweb_browser.sys.share

import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.util.asStream
import kotlinx.coroutines.flow.asFlow
import org.dweb_browser.helper.PromiseOut

actual suspend fun share(shareOptions: ShareOptions, multiPartData: MultiPartData?): String {
  val files = mutableListOf<String>()
  multiPartData?.forEachPart { partData ->
    when (partData) {
      is PartData.FileItem -> {
        partData.provider.asFlow().collect {
          it.asStream()

        }
        partData.originalFileName?.also { filename ->
          val url = CacheFilePlugin.writeFile(
            filename, EFileType.Cache, partData.streamProvider(), false
          )
          files.add(url)
        }
      }

      else -> {}
    }
    partData.dispose()
  }

  ShareController.controller.openActivity()
  ShareController.controller.waitActivityResultLauncherCreated()

  val result = PromiseOut<String>()
  SharePlugin.share(ShareController.controller, shareOptions, files, result)
  val data = result.waitPromise()
  debugShare("share", "result => $data")
  ShareController.controller.activity?.finish()
  return data
}

