package org.dweb_browser.sys.share

import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.core.readBytes
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.KmpNativeBridgeEventSender

actual suspend fun share(shareOptions: ShareOptions, multiPartData: MultiPartData?): String {

  return withMainContext {
    val files = multiPartData?.let {
      val listFile = mutableListOf<ByteArray>()
      multiPartData.forEachPart { partData ->
        val r = when (partData) {
          is PartData.FileItem -> {
            partData.provider.invoke().readBytes()
          }

          else -> {
            null
          }
        }

        r?.let {
          listFile.add(r)
        }

        partData.dispose()
      }
      listFile
    }

    KmpNativeBridgeEventSender.sendShare(
      shareOptions.title, shareOptions.text, shareOptions.url, files
    )
  }
}