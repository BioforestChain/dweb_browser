package org.dweb_browser.microservice.ipc.helper

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.http4k.core.Headers
import java.lang.reflect.Type

object IpcHeadersSerializer : KSerializer<IpcHeaders> {
  private val serializer = MapSerializer(String.serializer(), String.serializer())
  override val descriptor = serializer.descriptor

  override fun deserialize(decoder: Decoder) =
    IpcHeaders.from(decoder.decodeSerializableValue(serializer))

  override fun serialize(encoder: Encoder, value: IpcHeaders) {
    encoder.encodeSerializableValue(serializer, value.toMap())
  }

}

@Serializable(IpcHeadersSerializer::class)
@JsonAdapter(IpcHeaders::class)
class IpcHeaders(private val headersMap: MutableMap<String, String> = mutableMapOf()) :
  JsonSerializer<IpcHeaders>, JsonDeserializer<IpcHeaders> {
  companion object {
    fun from(headers: Map<String, String>) = IpcHeaders(headers.toMutableMap())
  }

  constructor(headers: Headers) : this() {
    for (header in headers) {
      header.second?.let {
        headersMap[header.first.asKey()] = it
      }
    }
  }

  fun set(key: String, value: String) {
    headersMap[key.asKey()] = value
  }

  fun init(key: String, value: String) {
    val headerKey = key.asKey()
    if (!headersMap.contains(headerKey)) {
      headersMap[headerKey] = value
    }
  }

  fun get(key: String): String? {
    return headersMap[key.asKey()]
  }

  fun getOrDefault(key: String, default: String) = headersMap[key.asKey()] ?: default

  fun has(key: String): Boolean {
    return headersMap.contains(key.asKey())
  }

  fun delete(key: String) {
    headersMap.remove(key.asKey())
  }

  fun forEach(fn: (key: String, value: String) -> Unit) {
    headersMap.forEach(fn)
  }

  fun toList(): List<Pair<String, String>> {
    return headersMap.toList()
  }

  fun toMap(): MutableMap<String, String> {
    return headersMap
  }

  override fun serialize(
    src: IpcHeaders, typeOfSrc: Type?, context: JsonSerializationContext
  ) = context.serialize(headersMap)

  override fun deserialize(
    json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext
  ) = IpcHeaders(context.deserialize<MutableMap<String, String>>(json, MutableMap::class.java))
}

private fun String.asKey(): String {
  return this.lowercase()
}
