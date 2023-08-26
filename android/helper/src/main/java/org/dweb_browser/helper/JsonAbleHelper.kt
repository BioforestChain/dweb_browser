package org.dweb_browser.helper

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.EmptySerializersModule


interface GsonAble<T> {
  fun toJsonAble(): com.google.gson.JsonElement
}

/**
 * 一种单向的可序列化对象
 */
interface JsonAble<T> {
  fun toJsonAble(): kotlinx.serialization.json.JsonElement
}

val JsonLoose = Json {
  ignoreUnknownKeys = true
}
