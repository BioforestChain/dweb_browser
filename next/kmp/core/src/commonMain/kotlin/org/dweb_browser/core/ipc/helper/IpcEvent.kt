package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.pure.http.PureBinaryFrame
import org.dweb_browser.pure.http.PureFrame
import org.dweb_browser.pure.http.PureTextFrame
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
  override fun toString() = "IpcEvent(name=$name, data=$encoding::$data)"

  companion object {
    fun fromBinary(name: String, data: ByteArray) = IpcEvent(name, data, IPC_DATA_ENCODING.BINARY)

    fun fromBase64(name: String, data: ByteArray) =
      IpcEvent(name, data.toBase64(), IPC_DATA_ENCODING.BASE64)

    fun fromUtf8(name: String, data: ByteArray) = fromUtf8(name, data.toUtf8())

    fun fromUtf8(name: String, data: String) = IpcEvent(name, data, IPC_DATA_ENCODING.UTF8)
    fun fromPureFrame(name: String, pureFrame: PureFrame) = when (pureFrame) {
      is PureTextFrame -> IpcEvent.fromUtf8(name, pureFrame.data)
      is PureBinaryFrame -> IpcEvent.fromBinary(name, pureFrame.data)
    }.also { it.pureFrame = pureFrame }


    private fun toPureFrame(ipcEvent: IpcEvent) = when (ipcEvent.encoding) {
      IPC_DATA_ENCODING.UTF8 -> PureTextFrame(ipcEvent.text)
      IPC_DATA_ENCODING.BINARY -> PureBinaryFrame(ipcEvent.binary)
      IPC_DATA_ENCODING.BASE64 -> PureBinaryFrame(ipcEvent.binary)
    }
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
    }.run {
      IpcEventJsonAble(
        name,
        data as String,
        encoding,
      )
    }
  }

  private var pureFrame: PureFrame? = null

  fun toPureFrame() = pureFrame ?: toPureFrame(this)
}