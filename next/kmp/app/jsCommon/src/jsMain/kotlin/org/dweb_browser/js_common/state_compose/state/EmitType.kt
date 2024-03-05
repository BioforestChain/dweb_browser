package org.dweb_browser.js_common.state_compose.state

import kotlinx.serialization.Serializable

@Serializable
data class EmitType (@JsName("value") val value: String){
    companion object{
        val REPLACE = EmitType("REPLACE")
        // only for ListComposeFlow
        val ADD = EmitType("ADD")
        val ADD_AT = EmitType("ADD_AT")
        val CLEAR = EmitType("CLEAR")
        val REMOVE = EmitType("REMOVE")
        val REMOVE_AT = EmitType("REMOVE_AT")
    }
}