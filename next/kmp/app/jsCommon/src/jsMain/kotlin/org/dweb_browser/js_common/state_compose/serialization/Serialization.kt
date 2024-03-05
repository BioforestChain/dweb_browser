package org.dweb_browser.js_common.state_compose.serialization

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Serialization<T: Any>(
    val encodeToString: (v: T) -> String,
    val decodeFromString: (v: String) -> T

){
//    inline fun<reified T> encodeToString(value: T) = Json.encodeToString(value)

    companion object{
        inline fun<reified T: Any> createInstance(): Serialization<T>{
            return Serialization(
                encodeToString = {
                    Json.encodeToString(it)
                }
            ){
                Json.decodeFromString<T>(it)
            }
        }
    }
}