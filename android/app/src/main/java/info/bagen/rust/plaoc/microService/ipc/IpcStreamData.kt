package info.bagen.rust.plaoc.microService.ipc

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import info.bagen.rust.plaoc.microService.helper.toBase64
import info.bagen.rust.plaoc.microService.helper.toUtf8
import java.lang.reflect.Type

@JsonAdapter(IpcStreamData::class)
data class IpcStreamData(
    override val stream_id: String,
    val data: Any /*String or ByteArray*/,
    val encoding: IPC_DATA_ENCODING
) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_DATA), IpcStream, JsonSerializer<IpcStreamData>,
    JsonDeserializer<IpcStreamData> {

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

    override fun serialize(
        src: IpcStreamData, typeOfSrc: Type, context: JsonSerializationContext
    ) = JsonObject().also { jsonObject ->
        with(src.jsonAble) {
            jsonObject.add("type", context.serialize(type))
            jsonObject.addProperty("stream_id", stream_id)
            jsonObject.addProperty("data", data as String)
            jsonObject.add("encoding", context.serialize(encoding))
        }
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): IpcStreamData = json.asJsonObject.let { obj ->
        IpcStreamData(
            stream_id = obj["stream_id"].asString,
            data = obj["data"].asString,
            encoding = context.deserialize(obj["encoding"], IPC_DATA_ENCODING::class.java)
        )
    }
}