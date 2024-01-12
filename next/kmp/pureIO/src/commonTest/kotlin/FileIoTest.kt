import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import org.dweb_browser.helper.consumeEachArrayRange
import kotlin.test.Test
import kotlin.test.assertContentEquals

class FileIoTest {
  @Test
  fun toReadByteStream() = runTest {
    val wroteByteArray = SystemFileSystem.sink(Path("/tmp/test.file")).run {
      val buffer = Buffer().apply {
        for (i in 0..2000) {
          writeInt(i)
        }
      }
      println("buffer.size: ${buffer.size}")
      write(buffer.copy(), buffer.size)
      buffer.readByteArray()
    }
    val fileSource = SystemFileSystem.source(Path("/tmp/test.file")).buffered()
    val channel = ByteReadChannel(fileSource.readByteArray())

    var readByteArray = byteArrayOf();
    println("start read")
    channel.consumeEachArrayRange { byteArray, last ->
      if (!last) {
        readByteArray += byteArray
        println("byteArray: ${byteArray.size}/${readByteArray.size}")
        delay(1000)
      }
    }

    assertContentEquals(readByteArray, wroteByteArray)
    delay(200)
  }
}