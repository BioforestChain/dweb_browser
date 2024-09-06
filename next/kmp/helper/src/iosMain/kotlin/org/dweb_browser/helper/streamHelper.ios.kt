package org.dweb_browser.helper

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSInputStream
import platform.posix.malloc
import platform.posix.uint8_tVar


@OptIn(ExperimentalForeignApi::class)
public fun NSInputStreamToByteReadChannel(
  scope: CoroutineScope,
  nsInputStream: NSInputStream,
): ByteReadChannel {
  val byteChannel = createByteChannel()
  val bytes = malloc(4096u) ?: throw Exception("malloc fail");
  scope.launch {
    while (nsInputStream.hasBytesAvailable) {
      val readSize = nsInputStream.read(bytes.reinterpret<uint8_tVar>(), 4096u)
      byteChannel.writePacket(ByteReadPacket(bytes.readBytes(readSize.toInt())))
    }
  }
  return byteChannel
}