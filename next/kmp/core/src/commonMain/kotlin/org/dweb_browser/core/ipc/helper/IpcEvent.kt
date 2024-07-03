package org.dweb_browser.core.ipc.helper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.helper.OrderBy
import org.dweb_browser.helper.base64String
import org.dweb_browser.helper.utf8String
import org.dweb_browser.pure.http.PureBinaryFrame
import org.dweb_browser.pure.http.PureFrame
import org.dweb_browser.pure.http.PureTextFrame


@Serializable
@SerialName(IPC_MESSAGE_TYPE_EVENT)
data class IpcEventRawString(
  val name: String,
  val data: String,
  val encoding: IPC_DATA_ENCODING,
  override val order: Int? = null,
) : IpcRawMessage, OrderBy {
  fun toIpcEvent() = IpcEvent(name, data, encoding, order)
}

@Serializable
@SerialName(IPC_MESSAGE_TYPE_EVENT)
data class IpcEventRawBinary(
  val name: String,
  val data: ByteArray,
  val encoding: IPC_DATA_ENCODING,
  override val order: Int? = null,
) : IpcRawMessage, OrderBy {
  fun toIpcEvent() = IpcEvent(name, data, encoding, order)
}

class IpcEvent(
  val name: String,
  val data: Any, /*String or ByteArray*/
  val encoding: IPC_DATA_ENCODING,
  override val order: Int? = null,
) : IpcMessage, OrderBy, RawAble<IpcRawMessage> {
  override fun toString() =
    "IpcEvent(name=$name, data=$encoding::${data.toString().trim()}, orderBy=$order)"

  companion object {
    fun fromBinary(name: String, data: ByteArray, orderBy: Int? = null) =
      IpcEvent(name, data, IPC_DATA_ENCODING.BINARY, orderBy)

    fun fromBase64(name: String, data: ByteArray, orderBy: Int? = null) =
      IpcEvent(name, data.base64String, IPC_DATA_ENCODING.BASE64, orderBy)

    fun fromUtf8(name: String, data: ByteArray, orderBy: Int? = null) =
      fromUtf8(name, data.utf8String, orderBy)

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

  override val stringAble by lazy {
    when (encoding) {
      IPC_DATA_ENCODING.BINARY -> IpcEventRawString(
        name,
        (data as ByteArray).base64String,
        IPC_DATA_ENCODING.BASE64,
        order,
      )

      IPC_DATA_ENCODING.BASE64, IPC_DATA_ENCODING.UTF8 -> IpcEventRawString(
        name,
        (data as String),
        encoding,
        order,
      )
    }
  }
  override val binaryAble by lazy {
    when (encoding) {
      IPC_DATA_ENCODING.BINARY -> IpcEventRawBinary(
        name,
        (data as ByteArray),
        encoding,
        order,
      )

      IPC_DATA_ENCODING.BASE64, IPC_DATA_ENCODING.UTF8 -> IpcEventRawBinary(
        name,
        binary,
        IPC_DATA_ENCODING.BINARY,
        order,
      )/**/
    }
  }

  private var pureFrame: PureFrame? = null

  fun toPureFrame() = pureFrame ?: toPureFrame(this)
}