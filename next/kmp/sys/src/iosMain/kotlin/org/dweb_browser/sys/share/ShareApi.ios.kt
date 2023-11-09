package org.dweb_browser.sys.share

import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.withMainContext
import org.dweb_browser.sys.KmpNativeBridgeEventSender

actual fun getShareController(): ShareApi = ShareIOSController()

class ShareIOSController(): ShareApi {
    override suspend fun share(
        title: String?,
        text: String?,
        url: String?,
        files: List<ByteArray>?
    ): String {

        val result =  PromiseOut<String>()

        val callback: ((String) -> Unit) = {
            println("[iOS] kmp did callback: $it")
            result.resolve(it)
        }

        withMainContext {
            KmpNativeBridgeEventSender.sendShare(title, text, url, files, callback)
        }

        return result.waitPromise()
    }
}