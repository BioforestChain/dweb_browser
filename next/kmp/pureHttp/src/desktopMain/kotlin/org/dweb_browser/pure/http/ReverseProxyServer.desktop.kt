package org.dweb_browser.pure.http

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.TcpSocketBuilder
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.isClosed
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.sockets.toJavaAddress
import io.ktor.util.network.port
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.ioAsyncExceptionHandler
import java.nio.ByteBuffer

val debugReverseProxy = Debugger("ReverseProxy")


class ReverseProxyServer {
  private var proxyServer: ServerSocket? = null
  private var acceptJob: Job? = null
  fun start(backendPort: UShort): UShort {
    val currentProxyServer = this.proxyServer ?: run {
      val tcpSocketBuilder = aSocket(ActorSelectorManager(ioAsyncExceptionHandler)).tcp()
      val proxyServer: ServerSocket
      val proxyHost = "0.0.0.0"
      val proxyPort = 0
      try {
        proxyServer = tcpSocketBuilder.bind(proxyHost, proxyPort) {
          reuseAddress = true
        }
      } catch (e: Exception) {
        throw IOException("Couldn't start proxy server on host [$proxyHost] and port [$proxyPort]\n\t${e.stackTraceToString()}")
      }
      println("Started proxy server at ${proxyServer.localAddress}")
      acceptJob = CoroutineScope(ioAsyncExceptionHandler).launch {
        while (!proxyServer.isClosed) {
          val client = proxyServer.accept()
          launch {
            try {
              val clientReader = client.openReadChannel()
              val clientWriter = client.openWriteChannel(true)

              // 等待内容填充
              clientReader.awaitContent()
              val buffer = ByteArray(clientReader.availableForRead)
              clientReader.readAvailable(buffer)
              val request = ConnectRequest(buffer)
              val connectHost: String
              val connectPort: Int
              if (request.host.endsWith(".dweb") && request.port == 443) {
                connectHost = "0.0.0.0"
                connectPort = backendPort.toInt()
              } else {
                connectHost = request.host
                connectPort = request.port
              }
              when {
                connectPort != -1 -> tunnelHttps(
                  connectHost,
                  connectPort,
                  client,
                  clientReader,
                  clientWriter,
                  tcpSocketBuilder
                )

                else -> {
                  println("Failed to connect to client or receive request from ${client.remoteAddress}")
                  // this blocks the coroutine's flow but whatever, it's the end of the coroutine now
                  client.close()
//                  cancel()
                }

              }
            } catch (e: Exception) {
              debugReverseProxy(
                "proxyServer-error",
                "Something went wrong during communicating with client",
                e.message
              )
              client.close()
              cancel()
            }
          }
        }
      }
      acceptJob?.invokeOnCompletion {
        WARNING("Proxy server closed ${proxyServer.isClosed}")
      }

      proxyServer
    }.also {
      this.proxyServer = it
    }
    return currentProxyServer.localAddress.toJavaAddress().port.toUShort()
  }

  fun close() {
    proxyServer?.let { serverSocket ->
      if (!serverSocket.isClosed) {
        serverSocket.close()
      }
    }
    proxyServer = null
  }

}


suspend fun tunnelHttps(
  connectHost: String,
  connectPort: Int,
  client: Socket,
  clientReader: ByteReadChannel,
  clientWriter: ByteWriteChannel,
  tcpSocketBuilder: TcpSocketBuilder,
) {
  val server: Socket?
  try {
    // 获取url并检查该url是否是 "optimizationguide-pa.googleapis.com"
    if (connectHost.contains("optimizationguide-pa.googleapis.com")) {
      return
    }
    server = tcpSocketBuilder.connect(connectHost, connectPort)
  } catch (e: Exception) {
    WARNING("Failed to connect to ${connectHost}:${connectPort}=> ${e.message}")
    withContext(Dispatchers.IO) {
      client.close()
    }
    return
  }
  println("Connected to ${connectHost}:${connectPort}")
  val serverReader = server.openReadChannel()
  val serverWriter = server.openWriteChannel()

  // tell the client that the connection is set and we are tunneling proxy
  val successConnectionString = "HTTP/1.1 200 OK\r\nProxyServer-agent: dweb-pure-http-proxy\r\n\r\n"
  clientWriter.writeAvailable(ByteBuffer.wrap(successConnectionString.toByteArray()))
  clientWriter.flush()

  coroutineScope {
    launch(ioAsyncExceptionHandler) {
      try {
        serverReader.copyTo(clientWriter)
      } catch (e: IOException) {
        WARNING("Failed to copy from server to client: ${e.message} [${connectHost}:${connectPort}]")
        serverReader.cancel(e)
        clientWriter.close(e)
        client.close()
        server.close()
        throw e
      }
    }
    launch(ioAsyncExceptionHandler) {
      try {
        clientReader.copyTo(serverWriter)
      } catch (e: IOException) {
        WARNING("Failed to copy from client to server: ${e.message} [${connectHost}:${connectPort}]")
        clientReader.cancel(e)
        serverWriter.close(e)
        client.close()
        server.close()
        throw e
      }
    }
  }
}

class ConnectRequest(buffer: ByteArray) {
  var host = ""
  var port = -1

  init {
    try {
      val content = buffer.decodeToString()
      for (line in content.splitToSequence(Regex("\n"))) {
        if (!line.startsWith("CONNECT ")) {
          continue
        }
        val origin = line.split(Regex("\\s+"))[1].split(":")
        host = origin.first()
        port = origin.getOrNull(1)?.toInt() ?: 80
        break
      }
    } catch (_: Throwable) {

    }
  }
}