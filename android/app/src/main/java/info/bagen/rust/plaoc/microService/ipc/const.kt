package info.bagen.rust.plaoc.microService.ipc

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import info.bagen.rust.plaoc.microService.helper.Callback
import info.bagen.rust.plaoc.microService.helper.asBase64
import info.bagen.rust.plaoc.microService.helper.asUtf8
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

@JsonAdapter(IPC_MESSAGE_TYPE::class)
enum class IPC_MESSAGE_TYPE(val type: Byte) : JsonSerializer<IPC_MESSAGE_TYPE>,
    JsonDeserializer<IPC_MESSAGE_TYPE> {
    /** 类型：请求 */
    REQUEST(0),

    /** 类型：相应 */
    RESPONSE(1),

    /** 类型：流数据，发送方 */
    STREAM_MESSAGE(2),

    /** 类型：流拉取，请求方 */
    STREAM_PULL(3),

    /** 类型：流关闭，发送方
     * 可能是发送完成了，也有可能是被中断了
     */
    STREAM_END(4),

    /** 类型：流中断，请求方 */
    STREAM_ABORT(5),

    /** 类型：事件 */
    STREAM_EVENT(6), ;


    override fun serialize(
        src: IPC_MESSAGE_TYPE, typeOfSrc: Type?, context: JsonSerializationContext?
    ): JsonElement = JsonPrimitive(src.type)


    override fun deserialize(
        json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?
    ): IPC_MESSAGE_TYPE = json.asByte.let { type -> values().first { it.type == type } }

}


@JsonAdapter(IPC_DATA_ENCODING::class)
enum class IPC_DATA_ENCODING(val encoding: Int) : JsonSerializer<IPC_DATA_ENCODING>,
    JsonDeserializer<IPC_DATA_ENCODING> {
    /** 文本 json html 等 */
    UTF8(1 shl 1),

    /** 使用文本表示的二进制 */
    BASE64(1 shl 2),

    /** 二进制 */
    BINARY(1 shl 3), ;


    override fun serialize(
        src: IPC_DATA_ENCODING, typeOfSrc: Type?, context: JsonSerializationContext?
    ) = JsonPrimitive(src.encoding)

    override fun deserialize(
        json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?
    ) = json.asInt.let { type -> values().find { it.encoding == type } }

}


@JsonAdapter(IPC_META_BODY_TYPE::class)
enum class IPC_META_BODY_TYPE(val type: Int) : JsonSerializer<IPC_META_BODY_TYPE>,
    JsonDeserializer<IPC_META_BODY_TYPE> {
    /** 流 */
    STREAM_ID(0),

    /** 内联数据 */
    INLINE(1),

    /** 文本 json html 等 */
    TEXT(INLINE or IPC_DATA_ENCODING.UTF8),

    /** 使用文本表示的二进制 */
    BASE64(INLINE or IPC_DATA_ENCODING.BASE64),

    /** 二进制 */
    BINARY(INLINE or IPC_DATA_ENCODING.BINARY), ;

    override fun serialize(
        src: IPC_META_BODY_TYPE, typeOfSrc: Type?, context: JsonSerializationContext?
    ) = JsonPrimitive(src.type)

    override fun deserialize(
        json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?
    ) = json.asInt.let { type -> values().find { it.type == type } }

    private inline infix fun or(TYPE: IPC_DATA_ENCODING) = type or TYPE.encoding
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

@JsonAdapter(MetaBody::class)
data class MetaBody(val type: IPC_META_BODY_TYPE, val data: Any, val ipcUid: Int) :
    JsonSerializer<MetaBody>,
    JsonDeserializer<MetaBody> {
    override fun serialize(
        src: MetaBody, typeOfSrc: Type, context: JsonSerializationContext
    ) = JsonArray().also {
        it.add(context.serialize(src.type))
        it.add(context.serialize(src.data))
        it.add(context.serialize(src.ipcUid))
    }

    override fun deserialize(
        json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext
    ) = json.asJsonArray.let { list ->
        context.deserialize<IPC_META_BODY_TYPE>(list[0], IPC_META_BODY_TYPE::class.java)
            .let { type ->
                when (type) {
                    IPC_META_BODY_TYPE.BINARY -> throw JsonParseException("json no support raw binary body")
                    else -> MetaBody(type, list[1].asString, list[2].asInt)
                }
            }
    }


    val binary
        get() = when (type) {
            IPC_META_BODY_TYPE.BINARY -> data as ByteArray
            IPC_META_BODY_TYPE.BASE64 -> (data as String).asBase64()
            IPC_META_BODY_TYPE.TEXT -> (data as String).asUtf8()
            else -> throw Exception("invalid metaBody.type :${type}")
        }


}

