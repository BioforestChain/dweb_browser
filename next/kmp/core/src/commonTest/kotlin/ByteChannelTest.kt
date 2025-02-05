package info.bagen.dwebbrowser

import io.ktor.utils.io.copyTo
import io.ktor.utils.io.writeByteArray
import kotlinx.coroutines.launch
import org.dweb_browser.helper.base64String
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.createByteChannel
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class ByteChannelTest {
  @Test
  fun writeBigByteArray() = runCommonTest {
    val source = createByteChannel()
    launch {
      source.writeByteArray(byteArray)
      source.close()
    }

    val sink = createByteChannel()
    launch {
      source.copyTo(sink)
      sink.close()
    }

    var res = byteArrayOf()
    sink.consumeEachArrayRange { byteArray, last ->
      println("res=${res.size} byteArray=${byteArray.size}")
      res += byteArray
    }

    assertContentEquals(res, byteArray)
  }

  companion object {
    val byteArray = byteArrayOf(1,2,3,4,5,6,7,8,9,10).base64String.repeat(20000).encodeToByteArray()
  }
}