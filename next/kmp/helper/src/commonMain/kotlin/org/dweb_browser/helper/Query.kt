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

class Query {

  companion object {
    val serializersModule: SerializersModule = EmptySerializersModule()

    class QueryValueItemDecoder(private val value: String) : Decoder {
      override val serializersModule: SerializersModule = Query.serializersModule
      override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
      }

      override fun decodeFloat(): Float {
        return value.toFloat()
      }

      override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        TODO("Not yet implemented")
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

    class QueryValuesDecoder(private val values: List<String>) : Decoder, CompositeDecoder {
      private var walkIndex = 0;
      override val serializersModule: SerializersModule = Query.serializersModule
      override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return this
      }

      override fun decodeBoolean(): Boolean {
        TODO("Not yet implemented")
      }

      override fun decodeByte(): Byte {
        TODO("Not yet implemented")
      }

      override fun decodeChar(): Char {
        TODO("Not yet implemented")
      }

      override fun decodeDouble(): Double {
        TODO("Not yet implemented")
      }

      override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        TODO("Not yet implemented")
      }

      override fun decodeFloat(): Float {
        TODO("Not yet implemented")
      }

      override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        TODO("Not yet implemented")
      }

      override fun decodeInt(): Int {
        TODO("Not yet implemented")
      }

      override fun decodeLong(): Long {
        TODO("Not yet implemented")
      }

      @ExperimentalSerializationApi
      override fun decodeNotNullMark(): Boolean {
        TODO("Not yet implemented")
      }

      @ExperimentalSerializationApi
      override fun decodeNull(): Nothing? {
        TODO("Not yet implemented")
      }

      override fun decodeShort(): Short {
        TODO("Not yet implemented")
      }

      override fun decodeString(): String {
        TODO("Not yet implemented")
      }

      override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        TODO("Not yet implemented")
      }

      override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        TODO("Not yet implemented")
      }

      override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        TODO("Not yet implemented")
      }

      override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        TODO("Not yet implemented")
      }

      override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (walkIndex >= values.size) {
          return CompositeDecoder.DECODE_DONE
        }

        return walkIndex++
      }

      override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        TODO("Not yet implemented")
      }

      override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
        TODO("Not yet implemented")
      }

      override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        TODO("Not yet implemented")
      }

      override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        TODO("Not yet implemented")
      }

      @ExperimentalSerializationApi
      override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
      ): T? {
        TODO("Not yet implemented")
      }

      override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
      ): T {
        return deserializer.deserialize(QueryValueItemDecoder(values[index]))
      }

      override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        TODO("Not yet implemented")
      }

      override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        TODO("Not yet implemented")
      }

      override fun endStructure(descriptor: SerialDescriptor) {

      }
    }

    class QueryDecoder(params: Parameters) :
      Decoder, CompositeDecoder {
      private val paramList = params.entries().toList()
      private var walkIndex = 0;
      override val serializersModule: SerializersModule = Query.serializersModule

      override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return paramList[index].value.first().toBoolean()
      }

      override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        return paramList[index].value.first().toByte()
      }

      override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        return paramList[index].value.first().toCharArray().first()
      }

      override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        return paramList[index].value.first().toDouble()
      }

      @OptIn(ExperimentalSerializationApi::class)
      override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val param = paramList.getOrNull(walkIndex++) ?: return CompositeDecoder.DECODE_DONE
        val index = descriptor.elementNames.indexOf(param.key)
        if (index == -1) {
          return CompositeDecoder.UNKNOWN_NAME
        }
        return index
      }

      override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        TODO("Not yet implemented")
      }

      override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder {
        TODO("Not yet implemented")
      }

      override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        TODO("Not yet implemented")
      }

      override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        TODO("Not yet implemented")
      }

      @ExperimentalSerializationApi
      override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
      ): T? {
        return null
      }

      override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
      ): T {
        return deserializer.deserialize(QueryValuesDecoder(paramList[index].value))
      }

      override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        return paramList[index].value.first().toShort()
      }

      override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        return paramList[index].value.first()
      }

      override fun endStructure(descriptor: SerialDescriptor) {

      }

      override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return this
      }

      override fun decodeBoolean(): Boolean {
        return paramList[walkIndex].value.first().toBoolean()
      }

      override fun decodeByte(): Byte {
        return paramList[walkIndex].value.first().toByte()
      }

      override fun decodeChar(): Char {
        return paramList[walkIndex].value.first().toCharArray().first()
      }

      override fun decodeDouble(): Double {
        return paramList[walkIndex].value.first().toDouble()
      }

      override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        TODO("Not yet implemented")
      }

      override fun decodeFloat(): Float {
        return paramList[walkIndex].value.first().toFloat()
      }

      override fun decodeInline(descriptor: SerialDescriptor): Decoder {
        TODO("Not yet implemented")
      }

      override fun decodeInt(): Int {
        return paramList[walkIndex].value.first().toInt()
      }

      override fun decodeLong(): Long {
        return paramList[walkIndex].value.first().toLong()
      }

      @ExperimentalSerializationApi
      override fun decodeNotNullMark(): Boolean {
        return paramList[walkIndex].value.first().toBooleanStrict()
      }

      @ExperimentalSerializationApi
      override fun decodeNull(): Nothing? {
        return null
      }

      override fun decodeShort(): Short {
        return paramList[walkIndex].value.first().toShort()
      }

      override fun decodeString(): String {
        return paramList[walkIndex].value.first()
      }
    }


    inline fun <reified T> decodeFromSearch(searchString: String) =
      decodeFromUrlParameters<T>(searchString.parseUrlEncodedParameters())

    inline fun <reified T> decodeFromUrl(url: Url) = decodeFromUrlParameters<T>(url.parameters)
    inline fun <reified T> decodeFromUrlParameters(parameters: Parameters): T {
      val decoder = QueryDecoder(parameters)
      return serializersModule.serializer<T>().deserialize(decoder)
    }
  }
}
