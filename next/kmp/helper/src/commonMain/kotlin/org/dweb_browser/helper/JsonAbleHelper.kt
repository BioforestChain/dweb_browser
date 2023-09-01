package org.dweb_browser.helper

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

interface GsonAble<T> {
  fun toJsonAble(): JsonElement
}

inline fun <reified T> T.toJsonElement() = Json.encodeToJsonElement<T>(this)


val JsonLoose = Json {
  ignoreUnknownKeys = true
}


open class StringEnumSerializer<T>(
  serialName: String, val ALL_VALUES: Map<String, T>, val getValue: T.() -> String
) : KSerializer<T> {
  override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
  override fun deserialize(decoder: Decoder) = ALL_VALUES.getValue(decoder.decodeString())
  override fun serialize(encoder: Encoder, value: T) = encoder.encodeString(value.getValue())
}

open class IntEnumSerializer<T>(
  serialName: String, val ALL_VALUES: Map<Int, T>, val getValue: T.() -> Int
) : KSerializer<T> {
  override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
  override fun deserialize(decoder: Decoder) = ALL_VALUES.getValue(decoder.decodeInt())
  override fun serialize(encoder: Encoder, value: T) = encoder.encodeInt(value.getValue())
}

open class ByteEnumSerializer<T>(
  serialName: String, val ALL_VALUES: Map<Byte, T>, val getValue: T.() -> Byte
) : KSerializer<T> {
  override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
  override fun deserialize(decoder: Decoder) = ALL_VALUES.getValue(decoder.decodeByte())
  override fun serialize(encoder: Encoder, value: T) = encoder.encodeByte(value.getValue())
}

open class ProxySerializer<T, P>(
  private val serializer: KSerializer<P>,
  private val valueToProxy: T.() -> P,
  private val proxyToValue: P.() -> T
) : KSerializer<T> {
  override val descriptor: SerialDescriptor = serializer.descriptor

  override fun serialize(encoder: Encoder, value: T): Unit =
    serializer.serialize(encoder, value.valueToProxy())

  override fun deserialize(decoder: Decoder): T = serializer.deserialize(decoder).proxyToValue()
}