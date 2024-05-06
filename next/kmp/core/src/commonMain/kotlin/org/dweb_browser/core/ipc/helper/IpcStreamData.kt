package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.toBase64

@Serializable()
@SerialName(IPC_MESSAGE_TYPE_STREAM_DATA)
class IpcStreamDataRawString(
  val stream_id: String, val encoding: IPC_DATA_ENCODING, val data: String,
) : IpcRawMessage {
  fun toIpcStreamData() = IpcStreamData(stream_id, encoding, data)
}


@Serializable()
@SerialName(IPC_MESSAGE_TYPE_STREAM_DATA)
class IpcStreamDataRawBinary(
  val stream_id: String, val encoding: IPC_DATA_ENCODING, val data: ByteArray,
) : IpcRawMessage {
  fun toIpcStreamData() = IpcStreamData(stream_id, encoding, data)
}


data class IpcStreamData(
  override val stream_id: String,
  val encoding: IPC_DATA_ENCODING,
  val data: Any, /*String or ByteArray*/
) : IpcMessage, IpcStream, RawAble<IpcRawMessage> {

  companion object {
    fun fromBinary(streamId: String, data: ByteArray) =
      IpcStreamData(streamId, IPC_DATA_ENCODING.BINARY, data)

    fun fromBase64(streamId: String, data: ByteArray) =
      IpcStreamData(streamId, IPC_DATA_ENCODING.BASE64, data.toBase64())

    fun fromUtf8(streamId: String, data: ByteArray) = fromUtf8(streamId, data.decodeToString())

    fun fromUtf8(streamId: String, data: String) =
      IpcStreamData(streamId, IPC_DATA_ENCODING.UTF8, data)
  }

  val binary by lazy {
    dataToBinary(data, encoding)
  }

  val text by lazy {
    dataToText(data, encoding)
  }

  override val stringAble by lazy {
    when (encoding) {
      IPC_DATA_ENCODING.BINARY -> IpcStreamDataRawString(
        stream_id, IPC_DATA_ENCODING.BASE64, (data as ByteArray).toBase64()
      )

      IPC_DATA_ENCODING.BASE64, IPC_DATA_ENCODING.UTF8 -> IpcStreamDataRawString(
        stream_id,
        encoding,
        data as String
      )
    }
  }
  override val binaryAble by lazy {
    when (encoding) {
      IPC_DATA_ENCODING.BINARY -> IpcStreamDataRawBinary(stream_id, encoding, data as ByteArray)
      IPC_DATA_ENCODING.BASE64, IPC_DATA_ENCODING.UTF8 -> IpcStreamDataRawBinary(
        stream_id, IPC_DATA_ENCODING.BINARY, binary
      )
    }
  }
}