package info.bagen.dwebbrowser

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.canReadContent
import org.dweb_browser.helper.readAvailableByteArray
import org.dweb_browser.microservice.ipc.helper.ReadableStream
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadableStreamTestTest {
  @OptIn(DelicateCoroutinesApi::class)
  @Test
  fun `readablestream available test`() = runBlocking {
    println("start")
    val stream = ReadableStream(onStart = { controller ->
      GlobalScope.launch {
        var i = 5
        while (i-- > 0) {
          controller.enqueue(byteArrayOf(i.toByte()))
          delay(400)
        }
        controller.close()
      }
    }, onClose = {
      println("onClose xxx")
    })

    var result by atomic(0)

    val reader = stream.stream.getReader("")
    while (reader.canReadContent()) {
      reader.awaitContent()
      println("availableForRead: ${reader.availableForRead}, isClosedForRead:${reader.isClosedForRead}, isClosedForWrite:${reader.isClosedForWrite}")
      val size = reader.readAvailableByteArray().size
      println("byteArray: $size")
      result += size
//      delay(200)
    }
//    reader.consumeEachArrayRange { byteArray, last ->
//      val size = byteArray.size
//      println("byteArray: $size")
//      result += size
//    }

    assertEquals(5, result)
  }
}