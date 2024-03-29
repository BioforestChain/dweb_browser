package org.dweb_browser.core.ipc.helper

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
//  Protobuf
}

sealed interface DWebMessage {
  val ports: List<IWebMessagePort>

  class DWebMessageString(
    val data: String,
    override val ports: List<IWebMessagePort> = emptyList()
  ) : DWebMessage

  class DWebMessageBytes(
    val data: ByteArray,
    override val ports: List<IWebMessagePort> = emptyList(),
    // TODO 这个编码要转移到应用层通讯协商那边
    val encode: DWebMessageBytesEncode = DWebMessageBytesEncode.Normal
  ) : DWebMessage
}