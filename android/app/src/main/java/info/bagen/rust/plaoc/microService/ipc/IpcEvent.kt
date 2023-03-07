package info.bagen.rust.plaoc.microService.ipc

import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import info.bagen.rust.plaoc.microService.helper.toBase64
import info.bagen.rust.plaoc.microService.helper.toUtf8
import java.lang.reflect.Type

class IpcEvent(val name: String, override val data: Any, override val encoding: IPC_DATA_ENCODING) :
    IpcMessage(IPC_MESSAGE_TYPE.EVENT), JsonSerializer<IpcEvent>, DataWithEncoding {

    companion object {
        inline fun fromBinary(name: String, data: ByteArray) =
            IpcEvent(name, data, IPC_DATA_ENCODING.BINARY)

        inline fun fromBase64(name: String, data: ByteArray) =
            IpcEvent(name, data.toBase64(), IPC_DATA_ENCODING.BASE64)

        inline fun fromUtf8(name: String, data: ByteArray) =
            fromUtf8(name, data.toUtf8())

        inline fun fromUtf8(name: String, data: String) =
            IpcEvent(name, data, IPC_DATA_ENCODING.UTF8)
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

}