package info.bagen.kmmsharedmodule.ipc

import info.bagen.kmmsharedmodule.helper.Callback
import info.bagen.kmmsharedmodule.helper.fromBase64
import info.bagen.kmmsharedmodule.helper.fromUtf8
import info.bagen.kmmsharedmodule.helper.toUtf8
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

data class IpcMessageArgs(val message: IpcMessage, val ipc: Ipc) {
    val component1 get() = message
    val component2 get() = ipc
}
typealias OnIpcMessage = Callback<IpcMessageArgs>

data class IpcRequestMessageArgs(val request: IpcRequest, val ipc: Ipc) {
    val component1 get() = request
    val component2 get() = ipc
}
typealias OnIpcRequestMessage = Callback<IpcRequestMessageArgs>

data class IpcEventMessageArgs(val event: IpcEvent, val ipc: Ipc) {
    val component1 get() = event
    val component2 get() = ipc
}
typealias OnIpcEventMessage = Callback<IpcEventMessageArgs>

//@JsonAdapter(IPC_MESSAGE_TYPE::class)
@Serializable
enum class IPC_MESSAGE_TYPE(val type: Byte) : KSerializer<IPC_MESSAGE_TYPE> {
    /** 类型：请求 */
    REQUEST(0),

    /** 类型：相应 */
    RESPONSE(1),

    /** 类型：流数据，发送方 */
    STREAM_DATA(2),

    /** 类型：流拉取，请求方 */
    STREAM_PULL(3),

    /** 类型：流关闭，发送方
     * 可能是发送完成了，也有可能是被中断了
     */
    STREAM_END(4),

    /** 类型：流中断，请求方 */
    STREAM_ABORT(5),

    /** 类型：事件 */
    EVENT(6), ;

    override fun serialize(encoder: Encoder, value: IPC_MESSAGE_TYPE) {
        encoder.encodeByte(value.type)
    }

    override fun deserialize(decoder: Decoder): IPC_MESSAGE_TYPE =
        decoder.decodeByte().let { type -> values().first { it.type == type } }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("IPC_MESSAGE_TYPE", PrimitiveKind.BYTE)
}

@Serializable
enum class IPC_DATA_ENCODING(val encoding: Int) : KSerializer<IPC_DATA_ENCODING> {
    /** UTF8编码的字符串，本质上是 BINARY */
    UTF8(1 shl 1),

    /** BASE64编码的字符串，本质上是 BINARY */
    BASE64(1 shl 2),

    /** 二进制, 与 UTF8/BASE64 是对等关系*/
    BINARY(1 shl 3),

//    /** BINARY-MESSAGEPACK 编码的二进制，本质上是 OBJECT */
//    MESSAGEPACK(1 shl 4),
//
//    /** UTF8-JSON 编码的字符串，本质上是 OBJECT */
//    JSON(1 shl 5),
//
//    /** 结构化对象，与 JSON、MessagePack 是对等关系 */
//    OBJECT(1 shl 6),
    ;

    override fun serialize(encoder: Encoder, value: IPC_DATA_ENCODING) {
        encoder.encodeInt(value.encoding)
    }

    override fun deserialize(decoder: Decoder): IPC_DATA_ENCODING =
        decoder.decodeInt().let { encoding -> values().first { it.encoding == encoding } }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("IPC_DATA_ENCODING", PrimitiveKind.INT)

}


@Serializable
enum class IPC_ROLE(val role: String) : KSerializer<IPC_ROLE> {
    SERVER("server"), CLIENT("client"), ;

    override fun serialize(encoder: Encoder, value: IPC_ROLE) {
        encoder.encodeString(value.role)
    }

    override fun deserialize(decoder: Decoder): IPC_ROLE =
        decoder.decodeString().let { role -> values().first { it.role == role } }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("IPC_ROLE", PrimitiveKind.STRING)
}


fun dataToBinary(
    data: Any /*String or ByteArray*/, encoding: IPC_DATA_ENCODING
) = when (encoding) {
    IPC_DATA_ENCODING.BINARY -> data as ByteArray
    IPC_DATA_ENCODING.BASE64 -> (data as String).fromBase64()
    IPC_DATA_ENCODING.UTF8 -> (data as String).fromUtf8()
    else -> throw Exception("unknown encoding")
}


fun dataToText(
    data: Any /*String or ByteArray*/, encoding: IPC_DATA_ENCODING
) = when (encoding) {
    IPC_DATA_ENCODING.BINARY -> (data as ByteArray).toUtf8()
    IPC_DATA_ENCODING.BASE64 -> (data as String).fromBase64().toUtf8()
    IPC_DATA_ENCODING.UTF8 -> data as String
    else -> throw Exception("unknown encoding")
}
