package org.dweb_browser.browser.zip

import io.ktor.http.HttpMethod
import okio.Path.Companion.toPath
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.core.std.file.FileNMM
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.ziplib.decompress

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
            "/decompress" bind HttpMethod.Get to defineStringResponse {
                val zipVfsPath = nativeFetch(
                    "file://file.std.dweb/realPath?path=${request.queryAs<String>("sourcePath")}"
                ).body.toPureString()
                val targetPath = request.queryAs<String>("targetPath")

                val pickerDirectory = "/picker/${randomUUID()}".toPath()
                val targetVfsPath = FileNMM.getVirtualFsPath(ipc.remote, targetPath)
                val pickerPathString = targetVfsPath.toVirtualPathString(pickerDirectory)

                nativeFetch(
                    "file://file.std.dweb/virtualPathRealPathMapping?pickerPath=${pickerDirectory}&realPath=${targetVfsPath}"
                )
                decompress(zipVfsPath, targetVfsPath.fsFullPath.toString())

                pickerPathString
            }
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }
}
