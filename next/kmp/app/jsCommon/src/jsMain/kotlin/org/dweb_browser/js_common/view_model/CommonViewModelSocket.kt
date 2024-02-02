package org.dweb_browser.js_common.view_model

import kotlinx.serialization.Serializable
@Serializable
data class SyncData(
    @JsName("key")
    val key: String,
    @JsName("value")
    val value: String,
    @JsName("type")
    val type: SyncType

)

@Serializable
class SyncType private constructor(@JsName("value") val value: String){
    override fun equals(other: Any?): Boolean {
        if(this === other) return true
        if(other !is SyncType) return false
        return this.value == other.value
    }
    companion object{
        val REPLACE = SyncType("REPLACE")
        val SET = SyncType("SET")
        val ADD = SyncType("ADD")
        val ADD_ALL = SyncType("ADD_ALL")
        val ADD_ALL_AT = SyncType("ADD_ALL_AT")
        val CLEAR = SyncType("CLEAR")
        val REMOVE = SyncType("REMOVE")
        val REMOVE_ALL = SyncType("REMOVE_ALL")
        val REMOVE_AT = SyncType("REMOVE_AT")
        val RETAIN_ALL = SyncType("RETAIN_ALL")
    }
}


