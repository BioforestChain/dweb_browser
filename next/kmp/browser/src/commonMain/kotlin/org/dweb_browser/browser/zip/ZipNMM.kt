package org.dweb_browser.browser.zip

import io.ktor.http.HttpMethod
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.helper.ImageResource

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
      "/decompress" bind HttpMethod.Get to defineBooleanResponse {
        val sourcePath = nativeFetch(
          "file://file.std.dweb/realPath?path=${request.query("sourcePath")}"
        ).text()
        val targetPath = request.query("targetPath")
        val targetVfsPath = FileNMM.getVirtualFsPath(ipc.remote, targetPath)
        decompress(sourcePath, targetVfsPath.fsFullPath.toString())
      }
    )
  }

  override suspend fun _shutdown() {
    TODO("Not yet implemented")
  }
}
