package info.bagen.dwebbrowser.microService.ipc

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import info.bagen.dwebbrowser.microService.helper.toBase64
import info.bagen.dwebbrowser.microService.helper.toUtf8ByteArray
import info.bagen.dwebbrowser.microService.helper.toUtf8
import java.lang.reflect.Type

@JsonAdapter(IpcEvent::class)
class IpcEvent(
    val name: String,
    val data: Any /*String or ByteArray*/,
    val encoding: IPC_DATA_ENCODING
) :
    IpcMessage(IPC_MESSAGE_TYPE.EVENT), JsonSerializer<IpcEvent>, JsonDeserializer<IpcEvent> {

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

    override fun serialize(
      src: IpcEvent, typeOfSrc: Type, context: JsonSerializationContext
    ) = JsonObject().also { jsonObject ->
        with(src.jsonAble) {
            jsonObject.add("type", context.serialize(type))
            jsonObject.addProperty("name", name)
            jsonObject.addProperty("data", data as String)
            jsonObject.add("encoding", context.serialize(encoding))
        }
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): IpcEvent = json.asJsonObject.let { obj ->
        IpcEvent(
            name = obj["name"].asString,
            data = obj["data"].asString,
            encoding = context.deserialize(obj["encoding"], IPC_DATA_ENCODING::class.java)
        )
    }

}