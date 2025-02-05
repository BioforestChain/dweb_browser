package org.dweb_browser.core.ipc.helper

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.dweb_browser.helper.ByteReadChannelDelegate
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.OrderDeferred
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.SuspendOnce1
import org.dweb_browser.helper.createByteChannel
import org.dweb_browser.pure.http.PureStream

val debugStream = Debugger("stream")

/**
 * 模拟Web的 ReadableStream
 */
class ReadableStream(
  val scope: CoroutineScope,
  cid: String? = null,
  val onOpenReader: suspend CoroutineScope.(arg: ReadableStreamController) -> Unit = {},
  val onClose: suspend CoroutineScope.() -> Unit = {},
  val onStart: CoroutineScope.(controller: ReadableStreamController) -> Unit = {},
) {
  /**
   * 内部的输出器
   */
  private val _stream = createByteChannel()

  class ReadableStreamChannel(
    val stream: ReadableStream,
    override val sourceByteReadChannel: ByteReadChannel,
  ) :
    ByteReadChannel by sourceByteReadChannel, ByteReadChannelDelegate {
    override fun cancel(cause: Throwable?) {
      return stream.closeRead(cause)
    }
  }

  val stream: PureStream = PureStream(ReadableStreamChannel(this, _stream))

  private val controller = ReadableStreamController(this)
  private val closePo = PromiseOut<Unit>()

  class ReadableStreamController(
    val stream: ReadableStream,
  ) {
    private val order = OrderDeferred()
    suspend fun enqueue(vararg byteArrays: ByteArray) = order.queueAndAwait("enqueue") {
      try {
        for (byteArray in byteArrays) {
          stream._stream.writeByteArray(byteArray)
        }
        true
      } catch (e: Throwable) {
        false
      }
    }

    suspend fun enqueue(byteArray: ByteArray) =
      order.queueAndAwait("enqueue(${byteArray.size}bytes)") {
        try {
          stream._stream.writeByteArray(byteArray)
          true
        } catch (e: Throwable) {
          false
        }
      }

    suspend fun enqueue(data: String) = enqueue(data.encodeToByteArray())

    inline fun background(crossinline handler: suspend () -> Unit) =
      stream.scope.launch(start = CoroutineStart.UNDISPATCHED) {
        handler()
      }

    suspend fun closeWrite(cause: Throwable? = null, interrupt: Boolean = false) {
      // 是否强制打断，如果是，那么进入到队列中
      if (interrupt) {
        stream.closeWrite(cause)
      } else {
        order.queueAndAwait("closeWrite($cause)") {
          stream.closeWrite(cause)
        }
      }
    }

    fun awaitClose(onClosed: suspend () -> Unit) {
      stream.scope.launch {
        stream.waitClosed()
        onClosed()
      }
    }
  }

  suspend fun waitClosed() = closePo.waitPromise()
  private fun emitClose() = scope.launch {
    if (!closePo.isResolved) {
      closePo.resolve(Unit)
      onClose(this)
    }
  }


  fun closeRead(reason: Throwable? = null) =
    _stream.cancel(reason).also {
      emitClose()
    }

  private val closeWrite = SuspendOnce1 { cause: Throwable? ->
    _stream.flush()
    _stream.close(cause).also {
      emitClose()
    }
  }


  companion object {
    private var id_acc by SafeInt(1)
  }

  val uid = "ReadableStream@${id_acc++}($cid)"

  override fun toString() = uid

  /// 生命周期
  init {
    scope.onStart(controller)
    scope.launch {
      stream.afterOpened.await()
      onOpenReader(controller)
    }
  }
}

class ReadableStreamOut(scope: CoroutineScope) {
  private lateinit var _controller: ReadableStream.ReadableStreamController
  val stream = ReadableStream(scope) {
    _controller = it
  }
  val controller get() = _controller
}
