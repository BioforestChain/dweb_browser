package org.dweb_browser.pure.io

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.bits.set
import io.ktor.utils.io.write
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.BufferedSink
import okio.BufferedSource
import okio.EOFException
import okio.FileSystem
import org.dweb_browser.helper.WARNING
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.createByteChannel
import org.dweb_browser.helper.globalIoScope
import org.dweb_browser.helper.ioAsyncExceptionHandler

expect val SystemFileSystem: FileSystem

fun BufferedSource.toByteReadChannel(scope: CoroutineScope = globalIoScope): ByteReadChannel {
  val channel = createByteChannel()
  scope.launch(ioAsyncExceptionHandler) {
    var end = false
    try {
      while (!end) {
        channel.write { freeSpace, startOffset, endExclusive ->
          var size = 0;
          for (i in startOffset..<endExclusive) {
            try {
              freeSpace[i] = readByte()
              size++
            } catch (e: EOFException) {
              end = true
              break
            }
          }
          size
        }
      }
    } catch (e: Exception) {
      WARNING("fileSystem error: ${e.message}")
    } finally {
      channel.close(null)
      close()
    }
  }
  return channel
}

suspend fun ByteReadChannel.copyTo(sink: BufferedSink) {
  consumeEachArrayRange { byteArray, last ->
    if (last) {
      sink.close()
    } else {
      sink.write(byteArray)
    }
  }
}
