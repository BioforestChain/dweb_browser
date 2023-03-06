package info.bagen.rust.plaoc.microService.ipc

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import info.bagen.rust.plaoc.microService.helper.asBase64
import info.bagen.rust.plaoc.microService.helper.asUtf8
import info.bagen.rust.plaoc.microService.helper.toBase64
import info.bagen.rust.plaoc.microService.helper.toUtf8
import java.lang.reflect.Type

@JsonAdapter(IpcStreamData::class)
data class IpcStreamData(
    val stream_id: String, val data: Any /*String or ByteArray*/, val encoding: IPC_DATA_ENCODING
) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_MESSAGE), JsonSerializer<IpcStreamData> {

    companion object {
        inline fun fromBinary(stream_id: String, data: ByteArray, ipc: Ipc) =
            if (ipc.supportBinary) asBinary(stream_id, data) else asBase64(stream_id, data)

        inline fun asBinary(stream_id: String, data: ByteArray) =
            IpcStreamData(stream_id, data, IPC_DATA_ENCODING.BINARY)

        inline fun asBase64(stream_id: String, data: ByteArray) =
            IpcStreamData(stream_id, data.toBase64(), IPC_DATA_ENCODING.BASE64)

        inline fun asUtf8(stream_id: String, data: ByteArray) =
            IpcStreamData(stream_id, data.toUtf8(), IPC_DATA_ENCODING.UTF8)
    }

    val binary
        get() = when (encoding) {
            IPC_DATA_ENCODING.BINARY -> data as ByteArray
            IPC_DATA_ENCODING.BASE64 -> (data as String).asBase64()
            IPC_DATA_ENCODING.UTF8 -> (data as String).asUtf8()
        }

    val jsonAble by lazy {
        when (encoding) {
            IPC_DATA_ENCODING.BINARY -> asBase64(
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
            jsonObject.add("stream_id", context.serialize(stream_id))
            jsonObject.add("data", context.serialize(data))
            jsonObject.add("encoding", context.serialize(encoding))
        }
    }
}