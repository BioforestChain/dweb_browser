package org.dweb_browser.js_common.network.socket

import kotlinx.serialization.Serializable


@Serializable
data class SyncState( @JsName("value")val value: String){
    companion object{
        val SERVER_TO_CLIENT_START = SyncState("SERVER_TO_CLIENT_START")
        val SERVER_TO_CLIENT_DONE = SyncState("SERVER_TO_CLIENT_DONE")
    }
}