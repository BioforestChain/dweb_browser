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
class IpcEvent(
    val name: String,
    val data: Any /*String or ByteArray*/,
    val encoding: IPC_DATA_ENCODING
) :
    IpcMessage(IPC_MESSAGE_TYPE.EVENT), KSerializer<IpcEvent> {

    companion object {
        inline fun fromBinary(name: String, data: ByteArray) =
            IpcEvent(name, data, IPC_DATA_ENCODING.BINARY)

        inline fun fromBase64(name: String, data: ByteArray) =
            IpcEvent(name, data.toBase64(), IPC_DATA_ENCODING.BASE64)

        inline fun fromUtf8(name: String, data: ByteArray) = fromUtf8(name, data.toUtf8())

        inline fun fromUtf8(name: String, data: String) =
            IpcEvent(name, data, IPC_DATA_ENCODING.UTF8)
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
                name,
                (data as ByteArray),
            )
            else -> this
        }
    }

    override fun serialize(encoder: Encoder, value: IpcEvent) {
        encoder.encodeStructure(descriptor) {
            encodeByteElement(descriptor, 0, value.type.type)
            encodeStringElement(descriptor, 1, value.name)
            encodeStringElement(descriptor, 2, value.data as String)
            encodeIntElement(descriptor, 3, value.encoding.encoding)
        }
    }

    override fun deserialize(decoder: Decoder): IpcEvent = decoder.decodeStructure(descriptor) {
        var type: IPC_MESSAGE_TYPE? = null
        var name: String? = null
        var data: String? = null
        var encoding: IPC_DATA_ENCODING? = null

        loop@ while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                DECODE_DONE -> break@loop
                0 -> {
                    type = when (decodeByteElement(descriptor, 0)) {
                        IPC_MESSAGE_TYPE.EVENT.type -> IPC_MESSAGE_TYPE.EVENT
                        else -> throw SerializationException("Unexpected IPC_MESSAGE_TYPE $index")
                    }
                }
                1 -> {
                    name = decodeStringElement(descriptor, 1)
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

        IpcEvent(name!!, data!!, encoding!!)
    }

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("IpcEvent") {
            element<IPC_MESSAGE_TYPE>("type")
            element<String>("name")
            element<String>("data")
            element<IPC_DATA_ENCODING>("encoding")
        }
}