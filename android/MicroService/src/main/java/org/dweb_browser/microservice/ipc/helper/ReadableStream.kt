package org.dweb_browser.microservice.ipc.helper

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.helper.*
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

fun debugStream(tag: String, msg: Any = "", err: Throwable? = null) =
  printDebug("stream", tag, msg, err)

/**
 * 模拟Web的 ReadableStream
 */
class ReadableStream(
  cid: String? = null,
  val onStart: (controller: ReadableStreamController) -> Unit = {},
  val onPull: suspend (arg: Pair<Int, ReadableStreamController>) -> Unit = {},
  val onClose: suspend () -> Unit = {},
) : InputStream() {

  // 数据源
  private var _data: ByteArray = byteArrayOf()
  private var ptr = 0 // 当前指针
  private val _dataLock = Mutex()

  class ReadableStreamController(
    val stream: ReadableStream,
  ) {
    suspend fun enqueue(byteArray: ByteArray) = stream.dataChannel.send(byteArray)
    suspend fun enqueue(data: String) = enqueue(data.toByteArray())

    fun enqueueBackground(byteArray: ByteArray) = stream.writeDataScope.launch {
      enqueue(byteArray)
    }

    fun enqueueBackground(data: String) = enqueueBackground(data.toByteArray())

    fun close() {
      stream.close()
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

  /**
   * 流协议不支持mark，读出来就直接丢了
   */
  override fun markSupported(): Boolean {
    return false
  }

  /** 执行垃圾回收
   * 10kb 的垃圾起，开始回收
   */
  private fun _gc() {
    runBlockingCatching(writeDataScope.coroutineContext) {
      _dataLock.withLock {
        if (ptr >= 1 /*10240*/ || isClosed) {
          debugStream("GC", "$uid => -${ptr} ~> ${_data.size - ptr}")
          _data = _data.sliceArray(ptr until _data.size)
          ptr = 0
        }
      }
    }.getOrNull()
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

  val canReadSize get() = _data.size - ptr

  /**
   * 读取数据，在尽可能满足下标读取的情况下
   */
  private fun requestData(requestSize: Int, waitFull: Boolean): ByteArray {
    val requestSize = if (waitFull) requestSize else 1
    // 如果下标满足条件，直接返回
    if (canReadSize >= requestSize) {
      return _data
    }
    // 如果还没有关闭，那就等待信号
    runBlockingCatching(readDataScope.coroutineContext) {// (readDataScope.coroutineContext)
      // 如果已经关闭，那么直接返回
      val wait = synchronized(this) {
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
              debugStream("REQUEST-DATA/END", "$uid => ${canReadSize}/$requestSize")
              wait.resolve(Unit) // 不需要抛出错误
            }

            canReadSize >= requestSize -> {
              debugStream(
                "REQUEST-DATA/CHANGED", "$uid => ${canReadSize}/$requestSize"
              )
              wait.resolve(Unit)
            }

            else -> {
              debugStream(
                "REQUEST-DATA/WAIT&PULL", "$uid => ${canReadSize}/$requestSize"
              )
              writeDataScope.launch {
                val desiredSize = requestSize - canReadSize
                onPull(Pair(desiredSize, controller))
              }
            }
          }
        }
      }

      wait.waitPromise()
      synchronized(this) {
        closeWaits.remove(wait)
      }
      counterJob.cancel()
      debugStream("REQUEST-DATA/DONE", "$uid => ${_data.size}")
    }.getOrNull()

    return _data
  }

  companion object {
    private var id_acc = AtomicInteger(1)
  }

  val uid = "#s${id_acc.getAndAdd(1)}${
    if (cid != null) {
      "($cid)"
    } else {
      ""
    }
  }"

  override fun toString() = uid


  /**
   * 抽象方法，必须实现
   */
  @Throws(IOException::class)
  override fun read(): Int {
    try {
      //当读到没有数据后，会返回-1
      val data = requestData(1, true)
      return if (ptr < data.size) data[ptr++].toInt() else -1
    } finally {
      _gc()
    }
  }

  /**
   * 可读数据长度
   */
  @Throws(IOException::class)
  override fun available(): Int {
    return (requestData(1, true).size - ptr).let { size ->
      synchronized(this) { if (isClosed && size == 0) -1 else size }
    }
  }

  @Throws(IOException::class)
  override fun close() = synchronized(this) {
    if (isClosed) {
      return
    }
    debugStream("CLOSE", uid)
    closePo.resolve(Unit)
    for (wait in closeWaits) {
      wait.resolve(Unit)
    }
    // 关闭的时候不会马上清空数据，还是能读出来最后的数据的

    super.close()
  }

  /**
   * 重写方法
   */
  @Throws(IOException::class)
  override fun read(b: ByteArray, off: Int, maxLen: Int): Int {
    try {
      val data = requestData(maxLen, false)
      var len = maxLen - off
      if (ptr >= data.size || len < 0) {
        //流已读完
        return -1
      }
      if (len == 0) {
        return 0
      }
      val availableLen = data.size - ptr

      //处理最后一次读取的时候可能不没有len的长度，取实际长度
      len = availableLen.coerceAtMost(len) // if (availableLen < len) availableLen else len
      System.arraycopy(data, ptr, b, off, len)
      ptr += len
      //返回读取的长度
      return len
    } finally {
      _gc()
    }
  }

  init {
    writeDataScope.launch {
      // 一直等待数据
      for (chunk in dataChannel) {
        _dataLock.withLock {
          _data += chunk
          debugStream("DATA-INIT", "$uid => +${chunk.size} ~> ${_data.size}")
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