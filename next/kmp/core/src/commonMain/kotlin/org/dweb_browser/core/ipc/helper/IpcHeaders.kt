package org.dweb_browser.core.ipc.helper

import io.ktor.http.Headers
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.dweb_browser.helper.ProxySerializer

object IpcHeadersSerializer :
  ProxySerializer<IpcHeaders, Map<String, String>>("IpcHeaders", MapSerializer(
    String.serializer(), String.serializer()
  ), { toMap() }, { IpcHeaders(this) })

@Serializable(IpcHeadersSerializer::class)
class IpcHeaders() {
  private val headersMap: MutableMap<String, String> = mutableMapOf()

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

  fun init(key: String, value: String): Boolean {
    val headerKey = key.asKey()
    if (!headersMap.contains(headerKey)) {
      headersMap[headerKey] = value
      return true
    }
    return false
  }

  fun get(key: String): String? {
    return headersMap[key.asKey()]
  }

  fun getOrDefault(key: String, default: String) = headersMap[key.asKey()] ?: default

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

  fun copy() = IpcHeaders(headersMap)
  operator fun iterator() = headersMap.iterator()
  private fun String.asKey(): String {
    return lowercase().split('-')
      .joinToString("-") { it.first().uppercaseChar() + it.substring(1) }
  }
}
