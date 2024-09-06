package org.dweb_browser.helper


import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.parseUrlEncodedParameters
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer

public class Query {

  public companion object {
    public val serializersModule: SerializersModule = EmptySerializersModule()

    public class QueryValueItemDecoder(private val value: String) : Decoder {
      override val serializersModule: SerializersModule = Query.serializersModule
      override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        TODO("Not yet implemented beginStructure")
      }

      override fun decodeBoolean(): Boolean {
        return value.toBoolean()
      }

      override fun decodeByte(): Byte {
        return value.toByte()
      }

      override fun decodeChar(): Char {
        return value.toCharArray().first()
      }

      override fun decodeDouble(): Double {
        return value.toDouble()
      }

      override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        TODO("Not yet implemented decodeEnum")
      }

      override fun decodeFloat(): Float {
        return value.toFloat()
      }

      override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        TODO("Not yet implemented decodeInline")
      }

      override fun decodeInt(): Int {
        return value.toInt()
      }

      override fun decodeLong(): Long {
        return value.toLong()
      }

      @ExperimentalSerializationApi
      override fun decodeNotNullMark(): Boolean {
        return value.toBooleanStrict()
      }

      @ExperimentalSerializationApi
      override fun decodeNull(): Nothing? {
        return null
      }

      override fun decodeShort(): Short {
        return value.toShort()
      }

      override fun decodeString(): String {
        return value
      }
    }

    public class QueryValuesDecoder(private val values: List<String>) : Decoder, CompositeDecoder {
      private var walkIndex = 0
      override val serializersModule: SerializersModule = Query.serializersModule
      override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return this
      }

      private val firstValue get() = values.first()
      private val firstValueOrNull get() = values.firstOrNull()

      override fun decodeBoolean(): Boolean = firstValue.toBoolean()

      override fun decodeByte(): Byte = firstValue.toByte()

      override fun decodeChar(): Char = firstValue[0]

      override fun decodeDouble(): Double = firstValue.toDouble()

      override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        TODO("Not yet implemented decodeEnum")
      }

      override fun decodeFloat(): Float = firstValue.toFloat()

      override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        TODO("Not yet implemented decodeInline")
      }

      override fun decodeInt(): Int = firstValue.toInt()

      override fun decodeLong(): Long = firstValue.toLong()

      @ExperimentalSerializationApi
      override fun decodeNotNullMark(): Boolean {
        TODO("Not yet implemented decodeNotNullMark")
      }

      @ExperimentalSerializationApi
      override fun decodeNull(): Nothing? {
        return when (val value = firstValueOrNull) {
          null -> null
          else -> if (value == "null" || value.isEmpty()) null else throw Exception("fail to decode to null: $value")
        }
      }

      override fun decodeShort(): Short = firstValue.toShort()

      override fun decodeString(): String = firstValue

      override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        TODO("Not yet implemented decodeBooleanElement")
      }

      override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        TODO("Not yet implemented decodeByteElement")
      }

      override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        TODO("Not yet implemented decodeCharElement")
      }

      override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        TODO("Not yet implemented decodeDoubleElement")
      }

      override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (walkIndex >= values.size) {
          return CompositeDecoder.DECODE_DONE
        }

        return walkIndex++
      }

      override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        TODO("Not yet implemented decodeFloatElement")
      }

      override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
        TODO("Not yet implemented decodeInlineElement")
      }

      override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        TODO("Not yet implemented decodeIntElement")
      }

      override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        TODO("Not yet implemented decodeLongElement")
      }

      @ExperimentalSerializationApi
      override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?,
      ): T? {
        TODO("Not yet implemented decodeNullableSerializableElement")
      }

      override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
      ): T {
        return deserializer.deserialize(QueryValueItemDecoder(values[index]))
      }

      override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        TODO("Not yet implemented decodeShortElement")
      }

      override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        TODO("Not yet implemented decodeStringElement")
      }

      override fun endStructure(descriptor: SerialDescriptor) {

      }
    }

    public class QueryDecoder(params: Parameters) : Decoder, CompositeDecoder {
      private val paramList = params.entries().toList()
      private var walkIndex = -1
      override val serializersModule: SerializersModule = Query.serializersModule
      private fun takeList() = paramList[walkIndex].value
      private fun takeFist() = paramList[walkIndex].value.first()

      override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return takeFist().toBoolean()
      }

      override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        return takeFist().toByte()
      }

      override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        return takeFist().toCharArray().first()
      }

      override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        return takeFist().toDouble()
      }

      @OptIn(ExperimentalSerializationApi::class)
      override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        while (true) {

          val param = paramList.getOrNull(++walkIndex) ?: return CompositeDecoder.DECODE_DONE
          val index = descriptor.elementNames.indexOf(param.key)
          if (index != -1) {
            return index
          }
          // 默认跳过未知参数
          // return CompositeDecoder.UNKNOWN_NAME
        }

      }

      override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        return takeFist().toFloat()
      }

      override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
        TODO("Not yet implemented decodeInlineElement")
      }

      override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        return takeFist().toInt()
      }

      override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        return takeFist().toLong()
      }

      @ExperimentalSerializationApi
      override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?,
      ): T? {
        return deserializer.deserialize(QueryValuesDecoder(takeList()))
      }

      override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
      ): T {
        return deserializer.deserialize(QueryValuesDecoder(takeList()))
      }

      override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        return takeFist().toShort()
      }

      override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        return takeFist()
      }

      override fun endStructure(descriptor: SerialDescriptor) {

      }

      override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return this
      }

      override fun decodeBoolean(): Boolean {
        return takeFist().toBoolean()
      }

      override fun decodeByte(): Byte {
        return takeFist().toByte()
      }

      override fun decodeChar(): Char {
        return takeFist().toCharArray().first()
      }

      override fun decodeDouble(): Double {
        return takeFist().toDouble()
      }

      override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        TODO("Not yet implemented decodeEnum")
      }

      override fun decodeFloat(): Float {
        return takeFist().toFloat()
      }

      override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        TODO("Not yet implemented decodeInline")
      }

      override fun decodeInt(): Int {
        return takeFist().toInt()
      }

      override fun decodeLong(): Long {
        return takeFist().toLong()
      }

      @ExperimentalSerializationApi
      override fun decodeNotNullMark(): Boolean {
        return takeFist().toBooleanStrict()
      }

      @ExperimentalSerializationApi
      override fun decodeNull(): Nothing? {
        return null
      }

      override fun decodeShort(): Short {
        return takeFist().toShort()
      }

      override fun decodeString(): String {
        return paramList[walkIndex].value.first()
      }
    }


    public inline fun <reified T> decodeFromSearch(searchString: String): T =
      decodeFromUrlParameters<T>(searchString.parseUrlEncodedParameters())

    public inline fun <reified T> decodeFromUrl(url: Url): T =
      decodeFromUrlParameters<T>(url.parameters)

    public inline fun <reified T> decodeFromUrlParameters(parameters: Parameters): T {
      val decoder = QueryDecoder(parameters)
      return serializersModule.serializer<T>().deserialize(decoder)
    }
  }
}
