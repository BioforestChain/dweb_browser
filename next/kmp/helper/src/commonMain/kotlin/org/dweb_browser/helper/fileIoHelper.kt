package org.dweb_browser.helper

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.bits.set
import io.ktor.utils.io.close
import io.ktor.utils.io.errors.EOFException
import io.ktor.utils.io.write
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okio.BufferedSink
import okio.BufferedSource
import okio.FileSystem

expect val SystemFileSystem: FileSystem

fun BufferedSource.toByteReadChannel(scope: CoroutineScope = CoroutineScope(ioAsyncExceptionHandler)): ByteReadChannel {
  val channel = ByteChannel(true)
  scope.launch {
    var end = false
    while (!end) {
      channel.write { freeSpace, startOffset, endExclusive ->
        var size = 0;
        println("write start $startOffset..<$endExclusive")
        for (i in startOffset..<endExclusive) {
          try {
            freeSpace[i] = readByte()
            size++
          } catch (e: EOFException) {
            end = true
            break
          }
        }
        println("write end $size")
        size
      }
    }
    channel.close()
    close()
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
