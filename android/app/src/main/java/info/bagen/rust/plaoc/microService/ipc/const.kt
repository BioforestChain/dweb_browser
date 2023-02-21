package info.bagen.rust.plaoc.microService.ipc

import com.google.gson.*
import info.bagen.rust.plaoc.microService.helper.Callback
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

enum class IPC_DATA_TYPE(val type: Byte) : JsonSerializer<IPC_DATA_TYPE>,
    JsonDeserializer<IPC_DATA_TYPE> {
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
    ;

    override fun serialize(
        src: IPC_DATA_TYPE,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement = JsonPrimitive(src.type)

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): IPC_DATA_TYPE = json.asByte.let { type -> values().first { it.type == type } }

}


enum class IPC_RAW_BODY_TYPE(val type: Int) : JsonSerializer<IPC_RAW_BODY_TYPE>,
    JsonDeserializer<IPC_RAW_BODY_TYPE> {
    /** 文本 json html 等 */
    TEXT(1 shl 1),

    /** 使用文本表示的二进制 */
    BASE64(1 shl 2),

    /** 二进制 */
    BINARY(1 shl 3),

    /** 流 */
    STREAM_ID(1 shl 4),

    /** 文本流 */
    TEXT_STREAM_ID(STREAM_ID.type or TEXT.type),

    /** 文本二进制流 */
    BASE64_STREAM_ID(STREAM_ID.type or BASE64.type),

    /** 二进制流 */
    BINARY_STREAM_ID(STREAM_ID.type or BINARY.type),
    ;

    override fun serialize(
        src: IPC_RAW_BODY_TYPE,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ) = JsonPrimitive(src.type)

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ) = json.asInt.let { type -> values().find { it.type == type } }

    infix fun and(TYPE: IPC_RAW_BODY_TYPE) = type and TYPE.type
}

enum class IPC_ROLE(val role: String) : JsonSerializer<IPC_ROLE>,
    JsonDeserializer<IPC_ROLE> {
    SERVER("server"),
    CLIENT("client"),
    ;

    override fun serialize(
        src: IPC_ROLE,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ) = JsonPrimitive(src.role)

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ) = json.asString.let { role -> values().find { it.role == role } }
}

class RawData(val type: IPC_RAW_BODY_TYPE, val data: Any) : JsonSerializer<RawData>,
    JsonDeserializer<RawData> {
    override fun serialize(
        src: RawData,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ) = JsonArray().also {
        it.add(context.serialize(src.type));
        it.add(context.serialize(src.data))
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext
    ) = json.asJsonArray.let { list ->
        context.deserialize<IPC_RAW_BODY_TYPE>(list[0], IPC_RAW_BODY_TYPE::class.java)
            .let { type ->
                when (type) {
                    IPC_RAW_BODY_TYPE.BINARY -> throw JsonParseException("json no support raw binary body")
                    else -> RawData(type, list[0].asString)
                }
            }
    }

}

