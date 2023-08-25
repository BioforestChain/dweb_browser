import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.microservice.ipc.helper.ReadableStream

/**
 * You can edit, run, and share this code.
 * play.kotlinlang.org
 */
fun main() {
  var b: Byte = 0
  val stream = ReadableStream(onPull = { (_, controller) ->
    Thread.sleep(500)
    controller.enqueue(byteArrayOf(b++))
    if (b > 10) {
      controller.close()
    }
  });


  MainScope().launch(ioAsyncExceptionHandler) {
    println("reading")
    while (stream.available() > 0) {
      println("read ${stream.read()}")
    }
  }
  runBlocking {
    stream.waitClosed()
  }
  println("DONE")
}
