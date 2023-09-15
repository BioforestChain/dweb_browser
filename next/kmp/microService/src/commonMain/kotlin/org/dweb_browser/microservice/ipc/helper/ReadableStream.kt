package org.dweb_browser.microservice.ipc.helper

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.writeAvailable
import io.ktor.utils.io.writeFully
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.toUtf8ByteArray
import org.dweb_browser.microservice.help.canRead
import org.dweb_browser.microservice.http.PureStream

fun debugStream(tag: String, msg: Any = "", err: Throwable? = null) = println("$tag $msg")
//  printDebug("stream", tag, msg, err)

/**
 * 模拟Web的 ReadableStream
 */
class ReadableStream(
  cid: String? = null,
  val onStart: (controller: ReadableStreamController) -> Unit = {},
  val onOpenReader: suspend (arg: ReadableStreamController) -> Unit = {},
  val onClose: suspend () -> Unit = {},
) {

  private val scope = CoroutineScope(CoroutineName("readableStream") + ioAsyncExceptionHandler)

  /**
   * 内部的写入器，这里不直接写入 output，是因为 需要 Channel 来作为写入缓冲器
   */
  private val input = Channel<ByteArray>()

  /**
   * 内部的输出器
   */
  private val output = ByteChannel(true)

  val stream: PureStream = PureStream(output)

  private val controller = ReadableStreamController(this)
  private val cancelPo = PromiseOut<Unit>()

  class ReadableStreamController(
    val stream: ReadableStream,
  ) {
    private val lock = Mutex()
    suspend fun enqueue(vararg byteArrays: ByteArray) = lock.withLock {
      for (byteArray in byteArrays) {
        stream.input.send(byteArray)
      }
    }

    suspend fun enqueue(byteArray: ByteArray) = lock.withLock { stream.input.send(byteArray) }
    suspend fun enqueue(data: String) = enqueue(data.toUtf8ByteArray())

    fun enqueueBackground(byteArray: ByteArray) = stream.scope.launch {
      enqueue(byteArray)
    }

    fun enqueueBackground(data: String) = enqueueBackground(data.toUtf8ByteArray())

    fun close() = stream.cancel()

    fun error(e: Throwable?) = stream.input.close(e)
    suspend fun awaitClose(function: suspend () -> Unit) {
      coroutineScope {
        launch {
          stream.waitCanceled()
          function()
        }
      }
    }
  }


  suspend fun waitCanceled() {
    cancelPo.waitPromise()
  }

  val isCanceled get() = cancelPo.isFinished

  fun cancel() {
    if (isCanceled) {
      return
    }
    debugStream("CLOSE", uid)
    // close Channel<ArrayBuffer>
    input.close()
    // cancel ReadableStream
    cancelPo.resolve(Unit)
  }

  companion object {
    private var id_acc by atomic(1)
  }

  val uid = "#s${id_acc++}${if (cid != null) "($cid)" else ""}"

  override fun toString() = uid


  /// 生命周期
  init {
    onStart(controller)
    scope.launch {
      stream.afterOpened.await()
      onOpenReader(controller)
    }
    scope.launch {
      // 一直等待数据
      for (chunk in input) {
        debugStream("DATA-IN", "$uid => +${chunk.size}")
        // chunk 可能很大，所以需要打包成 ByteReadPacket ，可以一点一点地写入
        output.writePacket(ByteReadPacket(chunk))
      }
      // close ByteChannel
      output.close()
      output.cancel()
      // 执行生命周期回调
      onClose()
    }
  }
}

class ReadableStreamOut {
  private lateinit var _controller: ReadableStream.ReadableStreamController
  val stream = ReadableStream(onStart = {
    _controller = it
  })
  val controller get() = _controller
}
