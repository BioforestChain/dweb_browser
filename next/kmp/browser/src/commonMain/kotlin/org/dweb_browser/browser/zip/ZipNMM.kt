package org.dweb_browser.browser.zip

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.WARNING

val debugZip = Debugger("ZipManager")

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
      "/decompress" bind PureMethod.GET by defineBooleanResponse {
        val sourcePath = nativeFetch(
          "file://file.std.dweb/realPath?path=${request.query("sourcePath")}"
        ).text()
        val targetPath = request.query("targetPath")
        // 先解压到一个临时目录
        val tmpVfsPath = "/data/tmp/${targetPath.substring(targetPath.lastIndexOf("/") + 1)}"
        // 获取真实目录
        val tmpPath = nativeFetch(
          "file://file.std.dweb/realPath?path=$tmpVfsPath"
        ).text()
        // 开始解压
        val ok = decompress(sourcePath, tmpPath)
        if (!ok) {
          return@defineBooleanResponse false
        }
        return@defineBooleanResponse nativeFetch(
          "file://file.std.dweb/move?sourcePath=${tmpVfsPath}&targetPath=${targetPath}"
        ).boolean()
      }
    )
  }

  override suspend fun _shutdown() {
    WARNING("Not yet implemented")
  }
}
