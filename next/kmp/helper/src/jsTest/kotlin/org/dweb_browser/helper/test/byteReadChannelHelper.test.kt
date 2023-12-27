package org.dweb_browser.helper.test

import io.ktor.utils.io.close
import io.ktor.utils.io.writeByte
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.dweb_browser.helper.commonConsumeEachArrayRange
import org.dweb_browser.helper.createByteChannel
import kotlin.test.Test

class ByteReadChannelHelperTest {
  @Test
  fun consumeEachArrayRange() = runTest {
    val byteChannel = createByteChannel()
    launch {
      for (i in 0..10) {
        byteChannel.writeByte(i)
        delay(100)
      }
      byteChannel.close()
    }
    byteChannel.commonConsumeEachArrayRange { byteArray, last ->
      println("byteArray=${byteArray.joinToString(",")} last=$last")
      delay(200)
    }
    println("okk~")
  }
}