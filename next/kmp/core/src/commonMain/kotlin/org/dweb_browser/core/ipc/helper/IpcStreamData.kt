package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.ProxySerializer
import org.dweb_browser.helper.toBase64
import org.dweb_browser.helper.toUtf8

@Serializable
data class IpcStreamDataJsonAble(
  val stream_id: String, val encoding: IPC_DATA_ENCODING, val data: String
) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_DATA) {
  fun toIpcStreamData() = IpcStreamData(stream_id, encoding, data)
}

object IpcStreamDataSerializer : ProxySerializer<IpcStreamData, IpcStreamDataJsonAble>(
  "IpcStreamData",
  IpcStreamDataJsonAble.serializer(),
  { jsonAble },
  { toIpcStreamData() })

@Serializable(IpcStreamDataSerializer::class)
data class IpcStreamData(
  override val stream_id: String,
  val encoding: IPC_DATA_ENCODING,
  val data: Any, /*String or ByteArray*/
) : IpcMessage(IPC_MESSAGE_TYPE.STREAM_DATA), IpcStream {

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
    }.run {
      IpcStreamDataJsonAble(stream_id, encoding, data as String)
    }
  }
}