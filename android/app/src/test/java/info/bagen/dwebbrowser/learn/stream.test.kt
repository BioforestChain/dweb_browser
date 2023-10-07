import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.canRead
import org.dweb_browser.microservice.ipc.helper.ReadableStream

/**
 * You can edit, run, and share this code.
 * play.kotlinlang.org
 */
fun main() {
  var b: Byte = 0
  val readableStream = ReadableStream(onOpenReader = { controller ->
    Thread.sleep(500)
    controller.enqueue(byteArrayOf(b++))
    if (b > 10) {
      controller.close()
    }
  });
  val reader = readableStream.stream.getReader("")


  MainScope().launch(ioAsyncExceptionHandler) {
    println("reading")
    while (reader.canRead) {
      println("read ${reader.readRemaining()}")
    }
  }
  runBlocking {
    readableStream.waitCanceled()
  }
  println("DONE")
}
