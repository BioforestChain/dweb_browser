package org.dweb_browser.sys.share

import io.ktor.http.HttpMethod
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.core.readBytes
import kotlinx.serialization.Serializable
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.receiveMultipart
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.printDebug
import org.dweb_browser.helper.toJsonElement

fun debugShare(tag: String, msg: Any? = "", err: Throwable? = null) = printDebug("Share", tag, msg, err)

@Serializable
data class ShareResult(val success: Boolean, val message: String)

class ShareNMM: NativeMicroModule("share.sys.dweb", "share") {
    init {
        categories = listOf(MICRO_MODULE_CATEGORY.Service, MICRO_MODULE_CATEGORY.Protocol_Service);
    }

    override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
        routes(
            /** 分享*/
            "/share" bind HttpMethod.Post to defineJsonResponse {

                val files = mutableListOf<ByteArray>()
                try {
                    val multiPartData = request.receiveMultipart()
                    multiPartData.forEachPart { partData ->
                        val r = when (partData) {
                            is PartData.FileItem -> {
                                partData.provider.invoke().readBytes()
                            }
                            else -> { null }
                        }

                        r?.let {
                            files.add(r)
                        }

                        partData.dispose()
                    }
                } catch (e: Exception) {
                    println("shareNMM files error: ${e.message}")
                }

                val result = getShareController().share(request.queryOrNull("title"), request.queryOrNull("text"), request.queryOrNull("url"), files)
                debugShare("share", "result => $result")
                return@defineJsonResponse ShareResult(result == "OK", result).toJsonElement()
            },
        )
    }

    override suspend fun _shutdown() {
        TODO("Not yet implemented")
    }

}