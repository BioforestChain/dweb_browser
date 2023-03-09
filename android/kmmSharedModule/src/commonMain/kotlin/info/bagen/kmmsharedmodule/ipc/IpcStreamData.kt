package info.bagen.kmmsharedmodule.ipc

import info.bagen.kmmsharedmodule.helper.toBase64
import info.bagen.kmmsharedmodule.helper.toUtf8
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE

@Serializable
data class IpcStreamData(
    val stream_id: String,
    val data: Any /*String or ByteArray*/,
    val encoding: IPC_DATA_ENCODING
) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_DATA), KSerializer<IpcStreamData> {

    companion object {
        inline fun fromBinary(stream_id: String, data: ByteArray) =
            IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY)

        inline fun fromBase64(stream_id: String, data: ByteArray) =
            IpcStreamData(stream_id, data.toBase64(), IPC_DATA_ENCODING.BASE64)

        inline fun fromUtf8(stream_id: String, data: ByteArray) = fromUtf8(stream_id, data.toUtf8())

        inline fun fromUtf8(stream_id: String, data: String) =
            IpcStreamData(stream_id, data, IPC_DATA_ENCODING.UTF8)
    }

    val binary by lazy {
        dataToBinary(data, encoding)
    }

    val text by lazy {
        dataToText(data, encoding)
    }

    val jsonAble by lazy {
        when (encoding) {
            IPC_DATA_ENCODING.BINARY -> fromBase64(
                stream_id,
                (data as ByteArray),
            )
            else -> this
        }
    }

    override fun serialize(encoder: Encoder, value: IpcStreamData) {
        encoder.encodeStructure(descriptor) {
            encodeByteElement(descriptor, 0, value.type.type)
            encodeStringElement(descriptor, 1, value.stream_id)
            encodeStringElement(descriptor, 2, value.data as String)
            encodeIntElement(descriptor, 3, value.encoding.encoding)
        }
    }

    override fun deserialize(decoder: Decoder): IpcStreamData = decoder.decodeStructure(descriptor) {
        var type: IPC_MESSAGE_TYPE? = null
        var stream_id: String? = null
        var data: String? = null
        var encoding: IPC_DATA_ENCODING? = null

        loop@ while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                DECODE_DONE -> break@loop
                0 -> {
                    type = when (decodeByteElement(descriptor, 0)) {
                        IPC_MESSAGE_TYPE.STREAM_DATA.type -> IPC_MESSAGE_TYPE.STREAM_DATA
                        else -> throw SerializationException("Unexpected IPC_MESSAGE_TYPE $index")
                    }
                }
                1 -> {
                    stream_id = decodeStringElement(descriptor, 1)
                }
                2 -> {
                    data = decodeStringElement(descriptor, 2)
                }
                3 -> {
                    encoding = when (decodeIntElement(descriptor, 3)) {
                        IPC_DATA_ENCODING.BINARY.encoding -> IPC_DATA_ENCODING.BINARY
                        IPC_DATA_ENCODING.BASE64.encoding -> IPC_DATA_ENCODING.BASE64
                        IPC_DATA_ENCODING.UTF8.encoding -> IPC_DATA_ENCODING.UTF8
                        else -> throw SerializationException("Unexpected IPC_MESSAGE_TYPE $index")
                    }
                }
                else -> throw SerializationException("Unexpected index $index")
            }
        }

//        if (name == null || data == null || encoding == null) throwMissingFieldException()

        IpcStreamData(stream_id!!, data!!, encoding!!)
    }

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("IpcStreamData") {
            element<IPC_MESSAGE_TYPE>("type")
            element<String>("stream_id")
            element<String>("data")
            element<IPC_DATA_ENCODING>("encoding")
        }
}