package org.dweb_browser.browser.zip

import io.ktor.http.HttpMethod
import okio.Path.Companion.toPath
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.consumeEachJsonLine
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.ziplib.unCompress

class ZipNMM : NativeMicroModule("zip.browser.dweb", "Zip") {
  init {
    short_name = "Zip存档管理"
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service
    )
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    routes(
      "/compress" bind HttpMethod.Get to defineStringResponse {
        ""
      },
      "/unCompress" bind HttpMethod.Get to defineStringResponse {
        val zipVfsPath = FileNMM.pickerPathToActualPathMap[request.queryAs("sourcePath")]
        val targetPath = request.queryAs<String>("targetPath")

        val pickerDirectory = "/picker/${randomUUID()}".toPath()
        val targetVfsPath = FileNMM.getVirtualFsPath(ipc.remote, targetPath)
        val pickerPathString = targetVfsPath.toVirtualPathString(pickerDirectory)

        val stream = nativeFetch("file://file.std.dweb/open?path=$zipVfsPath").body.toPureStream()
        stream.getReader("unCompress").consumeEachJsonLine<String> {
          unCompress("", "")
        }

        pickerPathString
      }
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }


}