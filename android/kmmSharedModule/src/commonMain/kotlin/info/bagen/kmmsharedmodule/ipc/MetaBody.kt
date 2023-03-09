package info.bagen.kmmsharedmodule.ipc

import info.bagen.kmmsharedmodule.helper.toBase64
import info.bagen.kmmsharedmodule.helper.toBase64Url
import java.util.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlin.random.Random

@Serializable
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
    /**
     * 唯一id，指代这个数据的句柄
     *
     * 需要使用这个值对应的数据进行缓存操作
     * 远端可以发送句柄回来，这样可以省去一些数据的回传延迟。
     */
    val metaId: String = ByteArray(8).also { Random().nextBytes(it) }.toBase64Url()
) : KSerializer<MetaBody> {

    @Serializable
    enum class IPC_META_BODY_TYPE(val type: Int) : KSerializer<IPC_META_BODY_TYPE> {
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

        override fun serialize(encoder: Encoder, value: IPC_META_BODY_TYPE) {
            encoder.encodeInt(value.type)
        }

        override fun deserialize(decoder: Decoder): IPC_META_BODY_TYPE =
            decoder.decodeInt().let { type -> values().first { it.type == type } }

        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("IPC_META_BODY_TYPE", PrimitiveKind.INT)

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

    override fun serialize(encoder: Encoder, value: MetaBody) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.type.type)
            encodeIntElement(descriptor, 1, value.senderUid)
            encodeStringElement(descriptor, 2, value.data as String)
            encodeStringElement(descriptor, 3, value.streamId)
            encodeIntElement(descriptor, 4, value.receiverUid)
            encodeStringElement(descriptor, 5, value.metaId)
        }
    }

    override fun deserialize(decoder: Decoder): MetaBody = decoder.decodeStructure(descriptor) {
        var type: IPC_META_BODY_TYPE? = null
        var senderUid: Int? = null
        var data: String? = null
        var streamId: String? = null
        var receiverUid: Int? = null
        var metaId: String? = null

        loop@ while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                DECODE_DONE -> break@loop
                0 -> {
                    type = when (decodeIntElement(descriptor, 0)) {
                        IPC_META_BODY_TYPE.INLINE.type -> IPC_META_BODY_TYPE.INLINE
                        IPC_META_BODY_TYPE.INLINE_BASE64.type -> IPC_META_BODY_TYPE.INLINE_BASE64
                        IPC_META_BODY_TYPE.INLINE_BINARY.type -> IPC_META_BODY_TYPE.INLINE_BINARY
                        IPC_META_BODY_TYPE.INLINE_TEXT.type -> IPC_META_BODY_TYPE.INLINE_TEXT
                        IPC_META_BODY_TYPE.STREAM_WITH_BASE64.type -> IPC_META_BODY_TYPE.STREAM_WITH_BASE64
                        IPC_META_BODY_TYPE.STREAM_ID.type -> IPC_META_BODY_TYPE.STREAM_ID
                        IPC_META_BODY_TYPE.STREAM_WITH_BINARY.type -> IPC_META_BODY_TYPE.STREAM_WITH_BINARY
                        IPC_META_BODY_TYPE.STREAM_WITH_TEXT.type -> IPC_META_BODY_TYPE.STREAM_WITH_TEXT
                        else -> throw SerializationException("Unexpected IPC_MESSAGE_TYPE $index")
                    }
                }
                1 -> {
                    senderUid = decodeIntElement(descriptor, 1)
                }
                2 -> {
                    data = decodeStringElement(descriptor, 2)
                }
                3 -> {
                    streamId = decodeStringElement(descriptor, 3)
                }
                4 -> {
                    receiverUid = decodeIntElement(descriptor, 4)
                }
                5 -> {
                    metaId = decodeStringElement(descriptor, 4)
                }
                else -> throw SerializationException("Unexpected index $index")
            }
        }

//        if (name == null || data == null || encoding == null) throwMissingFieldException()

        MetaBody(type!!, senderUid!!, data!!, streamId, receiverUid, metaId!!)
    }

    override val descriptor: SerialDescriptor
        get() = buildClassSerialDescriptor("MetaBody") {
            element<IPC_META_BODY_TYPE>("type")
            element<Int>("senderUid")
            element<String>("data")
            element<String>("streamId")
            element<Int>("receiverUid")
            element<String>("metaId")
        }
}


