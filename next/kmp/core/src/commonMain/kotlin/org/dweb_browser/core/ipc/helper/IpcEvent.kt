package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.Serializable
import org.dweb_browser.helper.ProxySerializer
import org.dweb_browser.helper.toBase64
import org.dweb_browser.pure.http.PureBinaryFrame
import org.dweb_browser.pure.http.PureFrame
import org.dweb_browser.pure.http.PureTextFrame


@Serializable
data class IpcEventJsonAble(
  val name: String, val data: String, val encoding: IPC_DATA_ENCODING, val orderBy: Int?,
) : IpcMessage(IPC_MESSAGE_TYPE.EVENT) {
  fun toIpcEvent() = IpcEvent(name, data, encoding)
}

object IpcEventSerializer :
  ProxySerializer<IpcEvent, IpcEventJsonAble>("IpcEvent", IpcEventJsonAble.serializer(),
    { jsonAble },
    { toIpcEvent() })

@Serializable(with = IpcEventSerializer::class)
class IpcEvent(
  val name: String,
  val data: Any, /*String or ByteArray*/
  val encoding: IPC_DATA_ENCODING,
  val orderBy: Int? = null,
) : IpcMessage(IPC_MESSAGE_TYPE.EVENT) {
  override fun toString() = "IpcEvent(name=$name, data=$encoding::${data.toString().trim()})"

  companion object {
    fun fromBinary(name: String, data: ByteArray, orderBy: Int? = null) =
      IpcEvent(name, data, IPC_DATA_ENCODING.BINARY, orderBy)

    fun fromBase64(name: String, data: ByteArray, orderBy: Int? = null) =
      IpcEvent(name, data.toBase64(), IPC_DATA_ENCODING.BASE64, orderBy)

    fun fromUtf8(name: String, data: ByteArray, orderBy: Int? = null) =
      fromUtf8(name, data.decodeToString(), orderBy)

    fun fromUtf8(name: String, data: String, orderBy: Int? = null) =
      IpcEvent(name, data, IPC_DATA_ENCODING.UTF8, orderBy)

    fun fromPureFrame(name: String, pureFrame: PureFrame, orderBy: Int? = null) = when (pureFrame) {
      is PureTextFrame -> fromUtf8(name, pureFrame.text, orderBy)
      is PureBinaryFrame -> fromBinary(name, pureFrame.binary, orderBy)
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
        orderBy
      )
    }
  }

  private var pureFrame: PureFrame? = null

  fun toPureFrame() = pureFrame ?: toPureFrame(this)
}