package org.dweb_browser.browser.zip

import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.file.ext.moveFile
import org.dweb_browser.core.std.file.ext.realPath
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.falseAlso
import org.dweb_browser.pure.http.PureMethod

val debugZip = Debugger("ZipManager")

class ZipNMM : NativeMicroModule("zip.browser.dweb", "Zip") {
  init {
    short_name = "Zip存档管理"
    categories = listOf(
      MICRO_MODULE_CATEGORY.Service
    )
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))
  }

  inner class ZipRuntime(override val bootstrapContext: BootstrapContext) : NativeRuntime() {
    override suspend fun _bootstrap() {
      routes(
        "/decompress" bind PureMethod.GET by defineEmptyResponse {
          val sourcePath = realPath(request.query("sourcePath"))
          val targetPath = realPath(request.query("targetPath"))
          // 先解压到一个临时目录
          val tmpVfsPath = "/data/tmp/${targetPath.name}"
          // 获取真实目录
          val tmpPath = realPath(tmpVfsPath)
          // 开始解压
          val ok = decompress(sourcePath.toString(), tmpPath.toString())
          if (!ok) {
            throwException(message = "decompress fail")
          }
          moveFile(tmpVfsPath, targetPath.toString()).falseAlso {
            throwException(message = "moveFile fail")
          }
        }
      )
    }

    override suspend fun _shutdown() {
    }
  }

  override fun createRuntime(bootstrapContext: BootstrapContext) = ZipRuntime(bootstrapContext)
}
