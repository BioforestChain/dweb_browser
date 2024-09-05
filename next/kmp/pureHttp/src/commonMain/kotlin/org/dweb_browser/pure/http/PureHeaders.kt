package org.dweb_browser.pure.http

import io.ktor.http.ContentType
import io.ktor.http.Headers
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.dweb_browser.helper.ProxySerializer
import org.dweb_browser.helper.SafeHashMap

object PureHeadersSerializer :
  ProxySerializer<PureHeaders, Map<String, String>>("IpcHeaders", MapSerializer(
    String.serializer(), String.serializer()
  ), { toMap() }, { PureHeaders(this) })

@Serializable(PureHeadersSerializer::class)
class PureHeaders() {
  private val headersMap = SafeHashMap<String, String>()

  constructor(headers: Map<String, String>) : this() {
    appendMap(headers.entries)
  }

  constructor(filter: List<Pair<String, String>>) : this() {
    appendPair(filter)
  }


  constructor(headers: Headers) : this() {
    for (header in headers.entries()) {
      headersMap[header.key.asKey()] = header.value.first()
    }
  }

  private fun appendMap(headers: Collection<Map.Entry<String, String>>) {
    for ((key, value) in headers) {
      headersMap[key.asKey()] = value
    }
  }

  private fun appendPair(headers: Collection<Pair<String, String>>) {
    for ((key, value) in headers) {
      headersMap[key.asKey()] = value
    }
  }

  fun set(key: String, value: String) {
    headersMap[key.asKey()] = value
  }

  fun setContentLength(len: Long) = set("Content-Length", len.toString())
  fun setContentLength(len: Int) = set("Content-Length", len.toString())
  fun setContentType(type: String) = set("Content-Type", type)
  fun setContentType(type: ContentType) = set("Content-Type", type.toString())

  fun init(key: String, value: String): Boolean {
    val headerKey = key.asKey()
    if (!headersMap.contains(headerKey)) {
      headersMap[headerKey] = value
      return true
    }
    return false
  }

  fun init(key: String, valueGetter: () -> String): Boolean {
    val headerKey = key.asKey()
    if (!headersMap.contains(headerKey)) {
      headersMap[headerKey] = valueGetter()
      return true
    }
    return false
  }

  fun get(key: String): String? {
    return headersMap[key.asKey()]
  }

  // add by jackie at 240205
  fun getByIgnoreCase(key: String): String? {
    return headersMap.filterKeys { mapKey ->
      mapKey.equals(key, true)
    }.values.firstOrNull()
  }

  fun getOrDefault(key: String, default: String) = headersMap[key.asKey()] ?: default
  fun getOrNull(key: String) = headersMap[key.asKey()]

  fun has(key: String): Boolean {
    return headersMap.contains(key.asKey())
  }

  fun delete(key: String) = headersMap.remove(key.asKey())

  fun forEach(fn: (Map.Entry<String, String>) -> Unit) {
    headersMap.forEach(fn)
  }

  fun toList(): List<Pair<String, String>> {
    return headersMap.toList()
  }

  fun toMap(): MutableMap<String, String> {
    return headersMap
  }

  fun toHttpHeaders() = headersMap.toList()

  fun copy() = PureHeaders(headersMap)
  operator fun iterator() = headersMap.iterator()
  private fun String.asKey(): String {
    return lowercase().split('-')
      .joinToString("-") { it.first().uppercaseChar() + it.substring(1) }
  }
}

fun PureHeaders.initCors() {
  init("Access-Control-Allow-Credentials", "true")
  init("Access-Control-Allow-Origin", "*")
  init("Access-Control-Allow-Headers", "*")
  init("Access-Control-Allow-Methods", "*")
}

fun PureHeaders.cors() = copy().initCors()