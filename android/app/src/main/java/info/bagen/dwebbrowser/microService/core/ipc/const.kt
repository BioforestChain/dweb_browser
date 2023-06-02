package info.bagen.dwebbrowser.microService.core.ipc

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import info.bagen.dwebbrowser.microService.helper.*
import java.lang.reflect.Type

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

data class IpcResponseMessageArgs(val response: IpcResponse, val ipc: Ipc) {
    val component1 get() = response
    val component2 get() = ipc
}
typealias OnIpcResponseMessage = Callback<IpcResponseMessageArgs>
data class IpcStreamMessageArgs(val response: IpcStream, val ipc: Ipc) {
    val component1 get() = response
    val component2 get() = ipc
}
typealias OnIpcStreamMessage = Callback<IpcStreamMessageArgs>

data class IpcEventMessageArgs(val event: IpcEvent, val ipc: Ipc) {
    val component1 get() = event
    val component2 get() = ipc
}
typealias OnIpcEventMessage = Callback<IpcEventMessageArgs>

@JsonAdapter(IPC_MESSAGE_TYPE::class)
enum class IPC_MESSAGE_TYPE(val type: Byte) : JsonSerializer<IPC_MESSAGE_TYPE>,
    JsonDeserializer<IPC_MESSAGE_TYPE> {
    /** 类型：请求 */
    REQUEST(0),

    /** 类型：相应 */
    RESPONSE(1),

    /** 类型：流数据，发送方 */
    STREAM_DATA(2),

    /** 类型：流拉取，请求方 */
    STREAM_PULL(3),

    /** 类型：推送流，发送方
     * 对方可能没有发送PULL过来，或者发送了被去重了，所以我们需要主动发送PUSH指令，对方收到后，如果状态允许，则会发送PULL指令过来拉取数据
     */
    STREAM_PAUSED(4),

    /** 类型：流关闭，发送方
     * 可能是发送完成了，也有可能是被中断了
     */
    STREAM_END(5),

    /** 类型：流中断，请求方 */
    STREAM_ABORT(6),

    /** 类型：事件 */
    EVENT(7),

    ;


    override fun serialize(
      src: IPC_MESSAGE_TYPE, typeOfSrc: Type?, context: JsonSerializationContext?
    ): JsonElement = JsonPrimitive(src.type)


    override fun deserialize(
        json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?
    ): IPC_MESSAGE_TYPE = json.asByte.let { type -> values().first { it.type == type } }

}


/**
 * 可预读取的流
 */
interface PreReadableInputStream {
    /**
     * 对标 InputStream.available 函数
     * 返回可预读的数据
     */
    val preReadableSize: Int
}
@JsonAdapter(IPC_DATA_ENCODING::class)
enum class IPC_DATA_ENCODING(val encoding: Int) : JsonSerializer<IPC_DATA_ENCODING>,
    JsonDeserializer<IPC_DATA_ENCODING> {
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


    override fun serialize(
      src: IPC_DATA_ENCODING, typeOfSrc: Type?, context: JsonSerializationContext?
    ) = JsonPrimitive(src.encoding)

    override fun deserialize(
        json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?
    ) = json.asInt.let { type -> values().find { it.encoding == type } }

}


@JsonAdapter(IPC_ROLE::class)
enum class IPC_ROLE(val role: String) : JsonSerializer<IPC_ROLE>, JsonDeserializer<IPC_ROLE> {
    SERVER("server"), CLIENT("client"), ;

    override fun serialize(
      src: IPC_ROLE, typeOfSrc: Type?, context: JsonSerializationContext?
    ) = JsonPrimitive(src.role)

    override fun deserialize(
        json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?
    ) = json.asString.let { role -> values().find { it.role == role } }
}


fun dataToBinary(
    data: Any /*String or ByteArray*/, encoding: IPC_DATA_ENCODING
) = when (encoding) {
    IPC_DATA_ENCODING.BINARY -> data as ByteArray
    IPC_DATA_ENCODING.BASE64 -> (data as String).toBase64ByteArray()
    IPC_DATA_ENCODING.UTF8 -> (data as String).toUtf8ByteArray()
    else -> throw Exception("unknown encoding")
}


fun dataToText(
    data: Any /*String or ByteArray*/, encoding: IPC_DATA_ENCODING
) = when (encoding) {
    IPC_DATA_ENCODING.BINARY -> (data as ByteArray).toUtf8()
    IPC_DATA_ENCODING.BASE64 -> (data as String).toBase64ByteArray().toUtf8()
    IPC_DATA_ENCODING.UTF8 -> data as String
    else -> throw Exception("unknown encoding")
}
