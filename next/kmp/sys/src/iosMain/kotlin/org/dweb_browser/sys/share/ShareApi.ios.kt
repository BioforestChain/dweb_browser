package org.dweb_browser.sys.share

import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.KmpNativeRegister
import org.dweb_browser.sys.KmpToIosEvent

actual fun getShareController(): ShareApi = ShareIOSController()

class ShareIOSController(): ShareApi {
    override suspend fun share(
        title: String?,
        text: String?,
        url: String?,
        files: List<ByteArray>?
    ): String {

        val result =  PromiseOut<String>()

        val inputDatas = mutableMapOf<String, Any>().apply {
            title?.let {
                this["title"] = title
            }
            text?.let {
                this["text"] = text
            }
            url?.let {
                this["url"] = url
            }
            files?.let {
                this["files"] = files
            }
        }

        val outputDatas = mutableMapOf<String, Any>().apply {
            val callback: ((String) -> Unit) = {
                println("[iOS Test] kmp did callback: $it")
                result.resolve(it)
            }
            this["callback"]= callback
        }

        withMainContext {
            val event = KmpToIosEvent("share", inputDatas, outputDatas)
            KmpNativeRegister.iOSImp?.invokeKmpEvent(event)
        }
        return result.waitPromise()
    }
}