package org.dweb_browser.microservice.ipc.helper

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeAvailable
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SimpleObserver
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.helper.toUtf8ByteArray
import org.dweb_browser.microservice.http.PureStream

fun debugStream(tag: String, msg: Any = "", err: Throwable? = null) = println("$tag $msg")
//  printDebug("stream", tag, msg, err)

/**
 * 模拟Web的 ReadableStream
 */
class ReadableStream(
  cid: String? = null,
  val onStart: (controller: ReadableStreamController) -> Unit = {},
  val onPull: suspend (arg: Pair<Int, ReadableStreamController>) -> Unit = {},
  val onClose: suspend () -> Unit = {},
) {

  private val channel = ByteChannel(true)
  val stream: PureStream = PureStream(channel)

  class ReadableStreamController(
    val stream: ReadableStream,
  ) {
    suspend fun enqueue(byteArray: ByteArray) = stream.dataChannel.send(byteArray)
    suspend fun enqueue(data: String) = enqueue(data.toUtf8ByteArray())

    fun enqueueBackground(byteArray: ByteArray) = stream.writeDataScope.launch {
      enqueue(byteArray)
    }

    fun enqueueBackground(data: String) = enqueueBackground(data.toUtf8ByteArray())

    fun close() {
      // close ByteWriteChannel
      stream.channel.close()
      // close ByteReadChannel
      stream.channel.cancel()
      // close ReadableStream
      stream.close()

      stream.dataChannel.close()
    }

    fun error(e: Throwable?) = stream.dataChannel.close(e)
    suspend fun awaitClose(function: suspend () -> Unit) {
      coroutineScope {
        launch {
          stream.waitClosed()
          function()
        }
      }
    }
  }

  private val dataChannel = Channel<ByteArray>()

  private val controller = ReadableStreamController(this)

  private val writeDataScope =
    CoroutineScope(CoroutineName("readableStream/writeData") + ioAsyncExceptionHandler)
  private val readDataScope =
    CoroutineScope(CoroutineName("readableStream/readData") + ioAsyncExceptionHandler)

  private val closePo = PromiseOut<Unit>()

  private val dataChangeObserver = SimpleObserver()

  suspend fun waitClosed() {
    closePo.waitPromise()
  }

  val isClosed get() = closePo.isFinished
  private val closeWaits = mutableListOf<PromiseOut<Unit>>()

  private val _lock = SynchronizedObject()

  fun close(): Unit {
    if (isClosed) {
      return
    }
    debugStream("CLOSE", uid)
    closePo.resolve(Unit)
    for (wait in closeWaits) {
      wait.resolve(Unit)
    }
  }

  /**
   * 读取数据，在尽可能满足下标读取的情况下
   */
  fun available(): Int {
    val requestSize = 1

    println("available: ${channel.availableForRead}")
    if (channel.availableForRead >= requestSize) {
      return channel.availableForRead
    }

    // 如果还没有关闭，那就等待信号
    runBlockingCatching(readDataScope.coroutineContext) {// (readDataScope.coroutineContext)
      // 如果已经关闭，那么直接返回
      val wait = synchronized(_lock) {
        if (isClosed) {
          return@runBlockingCatching
        }
        PromiseOut<Unit>().also {
          closeWaits.add(it)
        }
      }

      val counterJob = launch {
        dataChangeObserver.observe { count ->
          when {
            count == -1 -> {
              debugStream("REQUEST-DATA/END", "$uid => ${channel.availableForRead}/$requestSize")
              wait.resolve(Unit) // 不需要抛出错误
            }

            channel.availableForRead >= requestSize -> {
              debugStream(
                "REQUEST-DATA/CHANGED", "$uid => ${channel.availableForRead}/$requestSize"
              )
              wait.resolve(Unit)
            }

            else -> {
              debugStream(
                "REQUEST-DATA/WAIT&PULL", "$uid => ${channel.availableForRead}/$requestSize"
              )
              writeDataScope.launch {
                val desiredSize = requestSize - channel.availableForRead
                onPull(Pair(desiredSize, controller))
              }
            }
          }
        }
      }

      wait.waitPromise()
      synchronized(_lock) {
        closeWaits.remove(wait)
      }
      counterJob.cancel()
      debugStream("REQUEST-DATA/DONE", "$uid => ${channel.availableForRead}")
    }.getOrNull()

    return channel.availableForRead
  }

  companion object {
    private var id_acc = atomic(1)
  }

  val uid = "#s${id_acc.getAndAdd(1)}${
    if (cid != null) {
      "($cid)"
    } else {
      ""
    }
  }"

  override fun toString() = uid

  private val _dataLock = Mutex()

  init {
    writeDataScope.launch {
      // 一直等待数据
      for (chunk in dataChannel) {
        _dataLock.withLock {
          channel.writeAvailable(chunk)
          debugStream("DATA-INIT", "$uid => +${chunk.size} ~> ${channel.availableForRead}")
        }
        // 收到数据了，尝试解锁通知等待者
        dataChangeObserver.next()
      }
      // 关闭数据通道了，尝试解锁通知等待者
      dataChangeObserver.emit(-1)

      // 执行生命周期回调
      onClose()
    }
    onStart(controller)
  }
}

class ReadableStreamOut {
  private lateinit var _controller: ReadableStream.ReadableStreamController
  val stream = ReadableStream(onStart = {
    _controller = it
  })
  val controller get() = _controller
}