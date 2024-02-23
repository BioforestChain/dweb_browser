package org.dweb_browser.pure.http

import io.ktor.http.Url
import kotlinx.serialization.json.Json
import org.dweb_browser.helper.Query

interface PureUrl {
  val url: Url

  fun queryBoolean(key: String, default: Boolean = true) =
    this.url.parameters[key]?.let { it.lowercase() == "true" } ?: default

  fun queryOrNull(key: String) = this.url.parameters[key]
  fun query(key: String) = this.url.parameters[key] ?: throw Exception("No found search key:$key")

  companion object {

    fun queryOrNull(key: String): PureRequest.() -> String? = { queryOrNull(key) }
    fun <T> queryOrNull(key: String, transform: String.() -> T): PureRequest.() -> T? =
      { queryOrNull(key)?.run(transform) }

    fun query(key: String): PureRequest.() -> String = { query(key) }
    fun <T> query(key: String, transform: String.() -> T): PureRequest.() -> T =
      { query(key).run(transform) }
  }
}

fun Url.toPure() = object : PureUrl {
  override val url = this@toPure
}

inline fun <reified T> PureUrl.queryAs() = Query.decodeFromUrl<T>(this.url)
inline fun <reified T> PureUrl.queryAs(key: String) = Json.decodeFromString<T>(query(key))
inline fun <reified T> PureUrl.queryAsOrNull(key: String) =
  queryOrNull(key)?.let { Json.decodeFromString<T>(it) }