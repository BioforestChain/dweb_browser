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

    while(stream.available() > 0) {
      println("avaliable ${stream.stream.availableForRead}")

      val byteArray = stream.stream.readPacket(stream.stream.availableForRead).readByteArray()
      println("read: ${byteArray.toUtf8()}")
      result.incrementAndGet()
    }

//    delay(1000)
    assertEquals(5, result.value)
    assertEquals(0, stream.stream.availableForRead)
  }
}