package org.dweb_browser.pure.http

import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.writeByteArray
import kotlinx.io.Source
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.readAllAsByteArray

val debugKtor = Debugger("ktor")

var debugStreamAccId by SafeInt(1)

private suspend fun Source.copyToWithFlush(
  output: ByteWriteChannel, bufferSize: Int = DEFAULT_BUFFER_SIZE,
): Long {
  val id = debugStreamAccId++
  debugKtor("copyToWithFlush", "SS[$id] start")
  var bytesCopied: Long = 0
//  val buffer = ByteArray(bufferSize)
  try {
    do {
      when (val canReadSize = remaining) {
        0L, -1L -> {
          debugKtor("copyToWithFlush", "SS[$id] no byte!($canReadSize)")
          output.flush()
          break
        }

        else -> {
          debugKtor("copyToWithFlush", "SS[$id] can bytes($canReadSize)")
          val buffer = readAllAsByteArray()

          debugKtor("copyToWithFlush", "SS[$id] ${buffer.size}/$canReadSize bytes")
          if (buffer.isNotEmpty()) {
            bytesCopied += buffer.size
            output.writeByteArray(buffer)
            output.flush()
          } else {
            break
          }
        }
      }
    } while (true)
  } catch (e: Throwable) {
    // 有异常，那么可能是 output 的写入出现的异常，这时候需要将 input 也给关闭掉，因为已经不再读取了
    close()
    debugKtor("InputStream.copyToWithFlush", "", e)
  }
  debugKtor("copyToWithFlush", "SS[$id] end")
  return bytesCopied
}
