package org.dweb_browser.helper


interface GsonAble<T> {
  fun toJsonAble(): com.google.gson.JsonElement
}

/**
 * 一种单向的可序列化对象
 */
interface JsonAble<T> {
  fun toJsonAble(): kotlinx.serialization.json.JsonElement
}
