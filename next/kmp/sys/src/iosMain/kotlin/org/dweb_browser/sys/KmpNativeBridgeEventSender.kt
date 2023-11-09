package org.dweb_browser.sys

import kotlinx.coroutines.MainScope
import org.dweb_browser.helper.withMainContext

class KmpNativeBridgeEventSender {
    companion object {
        fun sendShare(
            title: String?,
            text: String?,
            url: String?,
            files: List<ByteArray>?,
            callback: ((String) -> Unit)
        ) {
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
            val outputDatas = mapOf("callback" to callback)
            val event = KmpToIosEvent("share", inputDatas, outputDatas)
            KmpNativeRegister.iOSImp?.invokeKmpEvent(event)
        }

        fun sendColorScheme(colorScheme: String) {
            val event =
                KmpToIosEvent("colorScheme", inputDatas = mapOf("colorScheme" to colorScheme), null)
            KmpNativeRegister.iOSImp?.invokeKmpEvent(event)
        }
    }
}