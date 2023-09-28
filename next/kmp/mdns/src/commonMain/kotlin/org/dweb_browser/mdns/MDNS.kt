package org.dweb_browser.mdns

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class MDNS {
  val selectorManager = SelectorManager(Dispatchers.IO)
  val serverSocket = aSocket(selectorManager).udp().bind(InetSocketAddress("127.0.0.1", 5353))

  init {
    CoroutineScope(Dispatchers.IO).launch {
      listen()
    }
  }

  suspend fun listen() {
    val reader = serverSocket.openReadChannel()
    reader.con
    while (reader.con)
    val input = serverSocket.receive()
    input.packet.r
  }
}