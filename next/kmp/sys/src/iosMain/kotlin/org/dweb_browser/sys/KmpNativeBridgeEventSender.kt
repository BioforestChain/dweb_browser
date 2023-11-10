package org.dweb_browser.sys

class KmpNativeBridgeEventSender {
    companion object {

        suspend fun sendShare(
            title: String?,
            text: String?,
            url: String?,
            files: List<ByteArray>?,
        ): String {
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
            val event = KmpToIosEvent("share", inputDatas)
            val result = KmpNativeRegister.iOSImp?.invokeAsyncKmpEvent(event)
            if (result != null) {
                return result as String ?: "Error"
            }
            return "Error"
        }

        fun sendColorScheme(colorScheme: String) {
            val event = KmpToIosEvent("colorScheme", inputDatas = mapOf("colorScheme" to colorScheme))
            KmpNativeRegister.iOSImp?.invokeKmpEvent(event)
        }
    }
}