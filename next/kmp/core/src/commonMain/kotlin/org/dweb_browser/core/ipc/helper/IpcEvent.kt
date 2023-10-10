package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.ProxySerializer
import org.dweb_browser.helper.toBase64
import org.dweb_browser.helper.toUtf8


@Serializable
data class IpcEventJsonAble(
  val name: String, val data: String, val encoding: IPC_DATA_ENCODING
) : IpcMessage(IPC_MESSAGE_TYPE.EVENT) {
  fun toIpcEvent() = IpcEvent(name, data, encoding)
}

object IpcEventSerializer :
  ProxySerializer<IpcEvent, IpcEventJsonAble>("IpcEvent", IpcEventJsonAble.serializer(),
    { jsonAble },
    { toIpcEvent() })

@Serializable(with = IpcEventSerializer::class)
class IpcEvent(
  val name: String, val data: Any /*String or ByteArray*/, val encoding: IPC_DATA_ENCODING
) : IpcMessage(IPC_MESSAGE_TYPE.EVENT) {

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
}