package org.dweb_browser.core.ipc.helper

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharedFlow

interface IWebMessageChannel {
  val port1: IWebMessagePort
  val port2: IWebMessagePort
}

interface IWebMessagePort {
  suspend fun start()
  suspend fun close(cause: CancellationException? = null)
  suspend fun postMessage(event: DWebMessage)
  val onMessage: SharedFlow<DWebMessage>
}

sealed interface DWebMessage {
  val text: String
  val binary: ByteArray
  val ports: List<IWebMessagePort>

  class DWebMessageString(
    data: String,
    override val ports: List<IWebMessagePort> = emptyList()
  ) : DWebMessage {
    override val text = data
    override val binary by lazy { text.encodeToByteArray() }
  }

  class DWebMessageBytes(
    data: ByteArray,
    override val ports: List<IWebMessagePort> = emptyList(),
  ) : DWebMessage {
    override val binary = data
    override val text by lazy { data.decodeToString() }
  }
}