package info.bagen.rust.plaoc.microService.ipc

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import info.bagen.rust.plaoc.microService.helper.toBase64
import info.bagen.rust.plaoc.microService.helper.toUtf8
import java.lang.reflect.Type

@JsonAdapter(IpcStreamData::class)
data class IpcStreamData(
    val stream_id: String,
    override val data: Any /*String or ByteArray*/,
    override val encoding: IPC_DATA_ENCODING
) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_MESSAGE), JsonSerializer<IpcStreamData>, DataWithEncoding {

    companion object {
        inline fun fromBinary(stream_id: String, data: ByteArray) =
            IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY)

        inline fun fromBase64(stream_id: String, data: ByteArray) =
            IpcStreamData(stream_id, data.toBase64(), IPC_DATA_ENCODING.BASE64)

        inline fun fromUtf8(stream_id: String, data: ByteArray) =
            fromUtf8(stream_id, data.toUtf8())

        inline fun fromUtf8(stream_id: String, data: String) =
            IpcStreamData(stream_id, data, IPC_DATA_ENCODING.UTF8)
    }

    @delegate:Transient
    val binary by lazy {
        dataToBinary()
    }

    @delegate:Transient
    val text by lazy {
        dataToText()
    }

    @delegate:Transient
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
}