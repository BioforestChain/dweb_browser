package org.dweb_browser.dwebview

import org.dweb_browser.helper.Signal

interface IWebMessageChannel {
  val port1: IWebMessagePort
  val port2: IWebMessagePort
}

interface IWebMessagePort {
  suspend fun start()
  suspend fun close()
  suspend fun postMessage(event: DWebMessage)
  val onMessage: Signal.Listener<DWebMessage>
}

enum class DWebMessageBytesEncode {
  Normal,
  Cbor,
  Protobuf
}

sealed class DWebMessage {
  data class DWebMessageString(
    val data: String,
    val ports: List<IWebMessagePort> = emptyList()
  ) : DWebMessage()

  data class DWebMessageBytes(
    val data: ByteArray,
    val ports: List<IWebMessagePort> = emptyList(),
    val encode: DWebMessageBytesEncode = DWebMessageBytesEncode.Normal
  ) : DWebMessage()
}