package info.bagen.rust.plaoc.microService.ipc

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import info.bagen.rust.plaoc.microService.helper.toBase64
import java.lang.reflect.Type
import java.util.*

@JsonAdapter(MetaBody::class)
data class MetaBody(
    /**
     * 类型信息，包含了 编码信息 与 形态信息
     * 编码信息是对 data 的解释
     * 形态信息（流、内联）是对 "是否启用 streamId" 的描述（注意，流也可以内联第一帧的数据）
     */
    val type: IPC_META_BODY_TYPE,
    val senderUid: Int,
    val data: Any,
    val streamId: String? = null,
    var receiverUid: Int? = null,
) : JsonSerializer<MetaBody> {

    @JsonAdapter(IPC_META_BODY_TYPE::class)
    enum class IPC_META_BODY_TYPE(val type: Int) : JsonSerializer<IPC_META_BODY_TYPE>,
        JsonDeserializer<IPC_META_BODY_TYPE> {
        /** 流 */
        STREAM_ID(0),

        /** 内联数据 */
        INLINE(1),


        /** 文本 json html 等 */
        STREAM_WITH_TEXT(STREAM_ID or IPC_DATA_ENCODING.UTF8),

        /** 使用文本表示的二进制 */
        STREAM_WITH_BASE64(STREAM_ID or IPC_DATA_ENCODING.BASE64),

        /** 二进制 */
        STREAM_WITH_BINARY(STREAM_ID or IPC_DATA_ENCODING.BINARY),

        /** 文本 json html 等 */
        INLINE_TEXT(INLINE or IPC_DATA_ENCODING.UTF8),

        /** 使用文本表示的二进制 */
        INLINE_BASE64(INLINE or IPC_DATA_ENCODING.BASE64),

        /** 二进制 */
        INLINE_BINARY(INLINE or IPC_DATA_ENCODING.BINARY), ;

        val encoding by lazy {
            val encoding = type and 0b11111110;
            IPC_DATA_ENCODING.values().find { it.encoding == encoding }
        }
        val isInline by lazy {
            type and 1 == 1
        }
        val isStream by lazy {
            type and 1 == 0
        }

        override fun serialize(
            src: IPC_META_BODY_TYPE, typeOfSrc: Type?, context: JsonSerializationContext?
        ) = JsonPrimitive(src.type)

        override fun deserialize(
            json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?
        ) = json.asInt.let { type -> values().find { it.type == type } }

        private inline infix fun or(TYPE: IPC_DATA_ENCODING) = type or TYPE.encoding
    }


    companion object {
        fun fromText(
            senderUid: Int,
            data: String,
            streamId: String? = null,
            receiverUid: Int? = null,
        ) = MetaBody(
            type = if (streamId == null) IPC_META_BODY_TYPE.INLINE_TEXT else IPC_META_BODY_TYPE.STREAM_WITH_TEXT,
            senderUid = senderUid,
            data = data,
            streamId = streamId,
            receiverUid = receiverUid,
        )

        fun fromBase64(
            senderUid: Int,
            data: String,
            streamId: String? = null,
            receiverUid: Int? = null,
        ) = MetaBody(
            type = if (streamId == null) IPC_META_BODY_TYPE.INLINE_BASE64 else IPC_META_BODY_TYPE.STREAM_WITH_BASE64,
            senderUid = senderUid,
            data = data,
            streamId = streamId,
            receiverUid = receiverUid,
        )

        fun fromBinary(
            senderUid: Int,
            data: ByteArray,
            streamId: String? = null,
            receiverUid: Int? = null,
        ) = MetaBody(
            type = if (streamId == null) IPC_META_BODY_TYPE.INLINE_BINARY else IPC_META_BODY_TYPE.STREAM_WITH_BINARY,
            senderUid = senderUid,
            data = data,
            streamId = streamId,
            receiverUid = receiverUid,
        )

        fun fromBinary(
            senderIpc: Ipc,
            data: ByteArray,
            streamId: String? = null,
            receiverUid: Int? = null,
        ) = if (senderIpc.supportBinary) fromBinary(
            senderIpc.uid, data, streamId, receiverUid
        ) else fromBase64(
            senderIpc.uid, data.toBase64(), streamId, receiverUid
        )
    }


    @delegate:Transient
    val jsonAble by lazy {
        when (type.encoding) {
            IPC_DATA_ENCODING.BINARY -> fromBase64(
                senderUid,
                (data as ByteArray).toBase64(),
                streamId,
                receiverUid
            )
            else -> this
        }
    }

    override fun serialize(
        src: MetaBody, typeOfSrc: Type, context: JsonSerializationContext
    ) = JsonObject().also { jsonObject ->
        with(src.jsonAble) {
            jsonObject.add("type", context.serialize(type))
            jsonObject.add("senderUid", context.serialize(senderUid))
            jsonObject.add("data", context.serialize(data))
            jsonObject.add("streamId", context.serialize(streamId))
            jsonObject.add("receiverUid", context.serialize(receiverUid))
        }
    }
}

private val JsonElement.asStringOrNull
    get() = if (isJsonNull) null else asString


private val JsonElement.asIntOrNull
    get() = if (isJsonNull) null else asInt

