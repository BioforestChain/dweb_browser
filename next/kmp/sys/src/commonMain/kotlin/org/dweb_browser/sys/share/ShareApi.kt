package org.dweb_browser.sys.share

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.PromiseOut

interface ShareApi {
    suspend fun share(title: String?, text: String?, url: String?, files: List<ByteArray>?): String
}

expect fun getShareController(): ShareApi