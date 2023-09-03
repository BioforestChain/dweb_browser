package org.dweb_browser.microservice.ipc.helper

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.ProxySerializer
import org.dweb_browser.helper.toBase64
import org.dweb_browser.helper.toUtf8
import java.lang.reflect.Type


@Serializable
data class IpcEventJsonAble(
  val name: String, val data: String, val encoding: IPC_DATA_ENCODING
) : IpcMessage(IPC_MESSAGE_TYPE.EVENT) {
  fun toIpcEvent() = IpcEvent(name, data, encoding)
}

object IpcEventSerializer :
  ProxySerializer<IpcEvent, IpcEventJsonAble>(IpcEventJsonAble.serializer(),
    { jsonAble },
    { toIpcEvent() })

@Serializable(with = IpcEventSerializer::class)
@JsonAdapter(IpcEvent::class)
class IpcEvent(
  val name: String, val data: Any /*String or ByteArray*/, val encoding: IPC_DATA_ENCODING
) : IpcMessage(IPC_MESSAGE_TYPE.EVENT), JsonSerializer<IpcEvent>, JsonDeserializer<IpcEvent> {

  companion object {
    fun fromBinary(name: String, data: ByteArray) = IpcEvent(name, data, IPC_DATA_ENCODING.BINARY)

    fun fromBase64(name: String, data: ByteArray) =
      IpcEvent(name, data.toBase64(), IPC_DATA_ENCODING.BASE64)

    fun fromUtf8(name: String, data: ByteArray) = fromUtf8(name, data.toUtf8())

    fun fromUtf8(name: String, data: String) = IpcEvent(name, data, IPC_DATA_ENCODING.UTF8)
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
    }.let {
      IpcEventJsonAble(
        name,
        data as String,
        encoding,
      )
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
    json: JsonElement, typeOfT: Type, context: JsonDeserializationContext
  ): IpcEvent = json.asJsonObject.let { obj ->
    IpcEvent(
      name = obj["name"].asString,
      data = obj["data"].asString,
      encoding = context.deserialize(obj["encoding"], IPC_DATA_ENCODING::class.java)
    )
  }

}