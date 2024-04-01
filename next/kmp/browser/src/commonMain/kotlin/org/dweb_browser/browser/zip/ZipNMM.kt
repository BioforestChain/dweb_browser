package org.dweb_browser.browser.zip

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.moveFile
import org.dweb_browser.core.std.file.ext.realFile
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.WARNING
import org.dweb_browser.pure.http.PureMethod

val debugZip = Debugger("ZipManager")

class ZipNMM : NativeMicroModule.NativeRuntime("zip.browser.dweb", "Zip") {
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
        val sourcePath = this@ZipNMM.realFile(request.query("sourcePath"))
        val targetPath = request.query("targetPath")
        // 先解压到一个临时目录
        val tmpVfsPath = "/data/tmp/${targetPath.substring(targetPath.lastIndexOf("/") + 1)}"
        // 获取真实目录
        val tmpPath = this@ZipNMM.realFile(tmpVfsPath)
        // 开始解压
        val ok = decompress(sourcePath, tmpPath)
        if (!ok) {
          return@defineBooleanResponse false
        }
        return@defineBooleanResponse this@ZipNMM.moveFile(tmpVfsPath, targetPath)
      }
    )
  }

  override suspend fun _shutdown() {
    WARNING("Not yet implemented")
  }
}
