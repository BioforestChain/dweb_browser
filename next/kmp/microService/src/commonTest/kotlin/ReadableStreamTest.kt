package info.bagen.dwebbrowser

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.helper.toUtf8
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

    val result = atomic(0)

    val readChannel = stream.stream.getReader()
    while(stream.available() > 0) {
      println("byteArray: ${readChannel.readPacket(readChannel.availableForRead).readByteArray()}")
      result.incrementAndGet()
    }

    assertEquals(5, result.value)
  }
}