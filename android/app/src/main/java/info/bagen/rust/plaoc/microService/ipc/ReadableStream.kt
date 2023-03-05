package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

inline fun debugStream(tag: String, msg: Any = "", err: Throwable? = null) =
    printdebugln("stream", tag, msg, err)

/**
 * 模拟Web的 ReadableStream
 */
class ReadableStream(
    cid: String? = null,
    val onStart: suspend (arg: ReadableStreamController) -> Unit = {},
    val onPull: suspend (arg: Pair<Int, ReadableStreamController>) -> Unit = {}
) : InputStream() {

    // 数据源
    private var _data: ByteArray = byteArrayOf()
    private var ptr = 0 // 当前指针
    private val _dataLock = Mutex()

    class ReadableStreamController(
        private val dataChannel: Channel<ByteArray>, val getStream: () -> ReadableStream
    ) {
        val stream get() = getStream()

        suspend fun enqueue(byteArray: ByteArray) = dataChannel.send(byteArray)

        fun close() {
            dataChannel.close()
        }

        fun error(e: Throwable?) = dataChannel.close(e)
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
        runBlocking(writeDataScope.coroutineContext) {
            _dataLock.withLock {
                if (ptr >= 10240 || isClosed) {
                    debugStream("GC/$uid", "-${ptr} ~> ${_data.size - ptr}")
                    _data = _data.sliceArray(ptr until _data.size)
                    ptr = 0
                }
            }
        }
    }


    private val dataChannel = Channel<ByteArray>()

    private val controller by lazy { ReadableStreamController(dataChannel) { this@ReadableStream } }

    private val writeDataScope =
        CoroutineScope(CoroutineName("readableStream/writeData") + ioAsyncExceptionHandler)
    private val readDataScope =
        CoroutineScope(CoroutineName("readableStream/readData") + ioAsyncExceptionHandler)

    init {
        runBlocking {
            onStart(controller)
        }
        writeDataScope.launch {
            // 一直等待数据
            for (chunk in dataChannel) {
                _dataLock.withLock {
                    _data += chunk
                    debugStream("DATA-IN/$uid", "+${chunk.size} ~> ${_data.size}")
                }
                // 收到数据了，尝试解锁通知等待者
                dataChangeObserver.next()
            }
            // 关闭数据通道了，尝试解锁通知等待者
            dataChangeObserver.emit(-1)

            // 执行关闭
            closePo.resolve(Unit)
        }
    }

    private val closePo = PromiseOut<Unit>()

    private val dataChangeObserver = SimpleObserver()

    suspend fun afterClosed() {
        closePo.waitPromise()
    }

    val isClosed get() = closePo.isFinished


    /**
     * 读取数据，在尽可能满足下标读取的情况下
     */
    private fun requestData(requestSize: Int): ByteArray {
        val ownSize = { _data.size - ptr }
        // 如果下标满足条件，直接返回
        if (ownSize() >= requestSize) {
            return _data
        }

        runBlocking {
            readDataScope.async {
                val wait = PromiseOut<Unit>()
                val counterJob = launch {
                    dataChangeObserver.observe { count ->
                        when {
                            count == -1 -> {
                                debugStream("REQUEST-DATA/END/$uid", "${ownSize()}/$requestSize")
                                wait.resolve(Unit) // 不需要抛出错误
                            }
                            ownSize() >= requestSize -> {
                                debugStream(
                                    "REQUEST-DATA/CHANGED/$uid", "${ownSize()}/$requestSize"
                                )
                                wait.resolve(Unit)
                            }
                            else -> {
                                debugStream(
                                    "REQUEST-DATA/WAITING-&-PULL/$uid", "${ownSize()}/$requestSize"
                                )
                                writeDataScope.launch {
                                    val desiredSize = requestSize - ownSize()
                                    onPull(Pair(desiredSize, controller))
                                }
                            }
                        }
                    }
                }
                wait.waitPromise()
                counterJob.cancel()
                debugStream("REQUEST-DATA/DONE/$uid", _data.size)
            }.join()
        }

        return _data
    }

    companion object {
        private var id_acc = AtomicInteger(1)
    }
//init {
//    when(id_acc.get()){
//        7,9->{
//            debugger()
//        }
//    }
//}

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
            val data = requestData(1)
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
        return requestData(1).size - ptr
    }

    @Throws(IOException::class)
    @Synchronized
    override fun close() {
        if (isClosed) {
            return
        }
        debugStream("CLOSE/${uid}")
        closePo.resolve(Unit)
        controller.close()
        // 关闭的时候不会马上清空数据，还是能读出来最后的数据的

        super.close()
    }

    /**
     * 重写方法
     */
    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        try {
            val data = requestData(len)
            var len = len
            if (ptr >= data.size || len < 0) {
                //流已读完
                return -1
            }
            if (len == 0) {
                return 0
            }

            //处理最后一次读取的时候可能不没有len的长度，取实际长度
            len = if (available() < len) available() else len
            System.arraycopy(data, ptr, b, off, len)
            ptr += len
            //返回读取的长度
            return len
        } finally {
            _gc()
        }
    }

}