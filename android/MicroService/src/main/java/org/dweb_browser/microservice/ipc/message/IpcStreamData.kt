package org.dweb_browser.microservice.ipc.message

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import org.dweb_browser.helper.toBase64
import org.dweb_browser.helper.toUtf8
import java.lang.reflect.Type

@JsonAdapter(IpcStreamData::class)
data class IpcStreamData(
  override val stream_id: String,
  val encoding: IPC_DATA_ENCODING,
  val data: Any /*String or ByteArray*/,
) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_DATA), IpcStream, JsonSerializer<IpcStreamData>,
  JsonDeserializer<IpcStreamData> {

  companion object {
    fun fromBinary(streamId: String, data: ByteArray) =
      IpcStreamData(streamId, IPC_DATA_ENCODING.BINARY, data)

    fun fromBase64(streamId: String, data: ByteArray) =
      IpcStreamData(streamId, IPC_DATA_ENCODING.BASE64, data.toBase64())

    fun fromUtf8(streamId: String, data: ByteArray) = fromUtf8(streamId, data.toUtf8())

    fun fromUtf8(streamId: String, data: String) =
      IpcStreamData(streamId, IPC_DATA_ENCODING.UTF8, data)
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