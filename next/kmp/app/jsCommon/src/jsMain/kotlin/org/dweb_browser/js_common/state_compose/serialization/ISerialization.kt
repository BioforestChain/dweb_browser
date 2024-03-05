//package org.dweb_browser.js_common.state_compose.serialization
//
//import kotlinx.serialization.encodeToString
//import kotlinx.serialization.json.Json
//interface ISerialization<T>{
//    fun encodeToString(value: T): String
//}
//
//
//inline fun<reified T> ISerialization.encodeToString(value: T): String{
//    return Json.encodeToString(value)
//}