package info.bagen.dwebbrowser

import io.ktor.utils.io.availableForRead
import io.ktor.utils.io.cancel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.core.ipc.helper.ReadableStream
import org.dweb_browser.helper.ByteReadChannelDelegate
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.canReadContent
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.createByteChannel
import org.dweb_browser.helper.readAvailableByteArray
import org.dweb_browser.test.runCommonTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReadableStreamTestTest {
  @OptIn(DelicateCoroutinesApi::class)
  @Test
  fun readableStreamAvailableTest() = runCommonTest {
    println("start")
    val stream = ReadableStream(this, onStart = { controller ->
      GlobalScope.launch {
        var i = 5
        while (i-- > 0) {
          controller.enqueue(byteArrayOf(i.toByte()))
          delay(400)
        }
        controller.closeWrite()
      }
    }, onClose = {
      println("onClose xxx")
    })

    var result by SafeInt(0)

    val reader = stream.stream.getReader("")
    while (reader.canReadContent()) {
      reader.awaitContent()
      println("availableForRead: ${reader.availableForRead}, isClosedForRead:${reader.isClosedForRead}")
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

  @Test
  fun testCancel() = runCommonTest {
    val readableStream = ReadableStream(this, onStart = { controller ->
      GlobalScope.launch {
        var i = 0
        while (true) {
          controller.enqueue(byteArrayOf(i++.toByte()))
          delay(400)
        }
      }
    });

    val reader = readableStream.stream.getReader("")
    var x = 0;

    launch {
      reader.consumeEachArrayRange { byteArray, last ->
        println("byteArray: ${byteArray.size}")
        if (x++ > 3) {
          breakLoop()
        }
      }
      reader.cancel();
    }

    readableStream.waitClosed()
    println("readableStream closed")
    assertTrue { true }
  }

  val byteArray = byteArrayOf(elements = (1..640000).distinct().map { it.toByte() }.toByteArray())

  @Test
  fun testSourceClose() = runCommonTest(1000) { time ->
    println("test-$time")

    val stream = ReadableStream(this, onStart = { controller ->
      launch(start = CoroutineStart.UNDISPATCHED) {
        controller.enqueue(byteArray)
      }
      launch {
        controller.closeWrite()
      }
    })
    val sink = createByteChannel()
    launch {
      when (val source = stream.stream.getReader("xix")) {
        is ByteReadChannelDelegate -> {
          source.sourceByteReadChannel
        }

        else -> source
      }.copyAndClose(sink)
    }

    var res = byteArrayOf()
    sink.consumeEachArrayRange { byteArray, last ->
      res += byteArray
    }

    assertContentEquals(res, byteArray)
  }

  @Test
  fun testSinkClose() = runCommonTest {

    val stream = ReadableStream(this, onStart = { controller ->
      launch(start = CoroutineStart.UNDISPATCHED) {
        println("enqueue start")
        controller.enqueue(byteArray)
        println("enqueue done")
      }
      launch {
        println("closeWrite start")
        controller.closeWrite()
        println("closeWrite done")
      }
    }, onClose = {
      println("onClose")
    })
    val sink = createByteChannel()
    launch {
      val source = stream.stream.getReader("xix")
      when (source) {
        is ByteReadChannelDelegate -> {
          source.sourceByteReadChannel
        }

        else -> source
      }.runCatching {
        copyAndClose(sink)
        println("copyAndClose done1")
      }.getOrElse {
        source.cancel(it)
      }

      println("copyAndClose done2")
    }

    var res = byteArrayOf()
    sink.consumeEachArrayRange { byteArray, last ->
      res += byteArray
      if (res.size > byteArray.size / 2) {
        sink.close()
        breakLoop()
      }
    }

    delay(100)
    println("okk")
  }
}