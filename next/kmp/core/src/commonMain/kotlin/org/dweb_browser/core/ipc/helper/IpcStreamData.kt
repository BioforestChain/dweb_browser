package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.OrderBy
import org.dweb_browser.helper.base64String
import org.dweb_browser.helper.utf8String

@Serializable()
@SerialName(IPC_MESSAGE_TYPE_STREAM_DATA)
data class IpcStreamDataRawString(
  val stream_id: String,
  val encoding: IPC_DATA_ENCODING,
  val data: String,
  override val order: Int,
) : IpcRawMessage, OrderBy {
  fun toIpcStreamData() = IpcStreamData(stream_id, encoding, data, order)
  override fun toString(): String {
    return "IpcStreamDataRawString(stream_id=$stream_id, encoding=$encoding, order=$order, data=${
      when (val len = data.length) {
        in 0..100 -> data
        else -> data.slice(0..19) + "..." + data.slice(len - 20..<len)
      }
    })"
  }
}


@Serializable()
@SerialName(IPC_MESSAGE_TYPE_STREAM_DATA)
data class IpcStreamDataRawBinary(
  val stream_id: String,
  val encoding: IPC_DATA_ENCODING,
  val data: ByteArray,
  override val order: Int,
) : IpcRawMessage, OrderBy {
  fun toIpcStreamData() = IpcStreamData(stream_id, encoding, data, order)
}


data class IpcStreamData(
  override val stream_id: String,
  val encoding: IPC_DATA_ENCODING,
  val data: Any, /*String or ByteArray*/
  override val order: Int,
) : IpcMessage, IpcStream, RawAble<IpcRawMessage>, OrderBy {

  override fun toString(): String {
    return "IpcStreamData(stream_id=$stream_id, encoding=$encoding, order=$order, data=${
      when (data) {
        is ByteArray -> "ByteArray[size=${data.size}]"
        is String -> when (val len = data.length) {
          in 0..100 -> data
          else -> data.slice(0..20) + "..." + data.slice(len - 20..<len)
        }

        else -> data.toString()
      }
    })"
  }

  companion object {
    fun fromBinary(streamId: String, data: ByteArray, order: Int = streamId.hashCode()) =
      IpcStreamData(streamId, IPC_DATA_ENCODING.BINARY, data, order)

    fun fromBase64(streamId: String, data: ByteArray, order: Int = streamId.hashCode()) =
      IpcStreamData(streamId, IPC_DATA_ENCODING.BASE64, data.base64String, order)

    fun fromUtf8(streamId: String, data: ByteArray, order: Int = streamId.hashCode()) =
      fromUtf8(streamId, data.utf8String, order)

    fun fromUtf8(streamId: String, data: String, order: Int = streamId.hashCode()) =
      IpcStreamData(streamId, IPC_DATA_ENCODING.UTF8, data, order)
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
        stream_id, IPC_DATA_ENCODING.BASE64, (data as ByteArray).base64String, order
      )

      IPC_DATA_ENCODING.BASE64, IPC_DATA_ENCODING.UTF8 -> IpcStreamDataRawString(
        stream_id, encoding, data as String, order
      )
    }
  }
  override val binaryAble by lazy {
    when (encoding) {
      IPC_DATA_ENCODING.BINARY -> IpcStreamDataRawBinary(
        stream_id, encoding, data as ByteArray, order
      )

      IPC_DATA_ENCODING.BASE64, IPC_DATA_ENCODING.UTF8 -> IpcStreamDataRawBinary(
        stream_id, IPC_DATA_ENCODING.BINARY, binary, order
      )
    }
  }
}