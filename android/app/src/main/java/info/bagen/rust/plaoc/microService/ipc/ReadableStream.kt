package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.Callback
import info.bagen.rust.plaoc.microService.helper.Signal
import info.bagen.rust.plaoc.microService.helper.printdebugln
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.io.InputStream

inline fun debugStream(tag: String, msg: Any, err: Throwable? = null) =
    printdebugln("stream", tag, msg, err)

/**
 * 模拟Web的 ReadableStream
 */
class ReadableStream(
    val onStart: Callback<ReadableStreamController> = {},
    val onPull: Callback<ReadableStreamController> = {}
) : InputStream() {

    private enum class StreamControlSignal {
        PULL,
    }

    class ReadableStreamController(
        private val dataChannel: Channel<ByteArray>,
        val stream: ReadableStream
    ) {

        suspend fun enqueue(byteArray: ByteArray) =
            dataChannel.send(byteArray)

        fun close() {
            dataChannel.close()
        }

        fun error(e: Throwable?) = dataChannel.close(e)
    }


    private val dataChannel = Channel<ByteArray>()
    private val controlSignal = Signal<StreamControlSignal>()

    private val controller = ReadableStreamController(dataChannel, this)

    private val pullScope = CoroutineScope(CoroutineName("readableStream/pull") + Dispatchers.IO)
    private val writeDataScope =
        CoroutineScope(CoroutineName("readableStream/writeData") + Dispatchers.IO)
    private val readDataScope =
        CoroutineScope(CoroutineName("readableStream/readData") + Dispatchers.IO)

    init {
        runBlocking {
            onStart(controller)
        }
        controlSignal.listen { signal ->
            when (signal) {
                StreamControlSignal.PULL -> {
                    debugStream("PULL/START/${uid}", currentCoroutineContext().toString())
                    onPull(controller)
                    debugStream("PULL/END/${uid}", currentCoroutineContext().toString())
                }
            }
        }
        writeDataScope.launch {
            // 一直等待数据
            for (chunk in dataChannel) {
                _data += chunk
                // 收到数据了，尝试解锁通知等待者
                tryUnlock()
            }
            // 关闭数据通道了，尝试解锁通知等待者
            tryUnlock()
            closed = true
            closeLock.unlock()
        }
    }

    private val closeLock = Mutex(true)
    private var closed = false
    private val dataLock = Mutex()
    private inline fun tryUnlock() {
        if (dataLock.isLocked) {
            dataLock.unlock()
            debugStream("TRY-UNLOCK/${uid}", _data.size)
        }
    }

    suspend fun closed() {
        if (closed) return
        closeLock.withLock { }
    }


    /**
     * 读取数据，在尽可能满足下标读取的情况下
     */
    private fun requestData(ptr: Int): ByteArray {
        // 如果下标满足条件，直接返回
        if (ptr < _data.size) {
            return _data
        }

        // 如果还能从控制端读取数据，那么等待数据写入
        if (!dataChannel.isClosedForSend) {
            // 数据不够了，发送拉取的信号
            runBlocking(readDataScope.coroutineContext) {
                debugStream("REQUEST-DATA/LOCK/${uid}", _data.size)
                // 数据不够了，发送拉取的信号
                controlSignal.emit(StreamControlSignal.PULL)
                dataLock.lock()
                debugStream("REQUEST-DATA/UNLOCK/${uid}", _data.size)
            }
            return requestData(ptr)
        }

        // 控制端无法读取数据了，只能直接返回
        return _data
    }

    companion object {
        private var id_acc = 0
    }

    private val uid = "#${id_acc++}"
    override fun toString() = uid


    // 数据源
    private var _data: ByteArray = byteArrayOf()
    private var ptr = 0 // 当前指针
    private var mark = 0 //标记


    /**
     * 抽象方法，必须实现
     */
    @Throws(IOException::class)
    override fun read(): Int {
        //当读到没有数据后，会返回-1
        val data = requestData(ptr)
        return if (ptr < data.size) data[ptr++].toInt() else -1
    }

    /**
     * 可读数据长度
     */
    @Throws(IOException::class)
    override fun available(): Int {
        return requestData(ptr).size - ptr
    }

    @Throws(IOException::class)
    override fun close() {
        super.close()
        controller.close()
        ptr = _data.size
    }

    /**
     * 标记，与reset相应
     */
    @Synchronized
    override fun mark(readlimit: Int) {
        mark = readlimit
    }

    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        if (mark < 0 || mark >= _data.size) {
            throw IOException("标识不对")
        }
        ptr = mark //指针重新指到mark位置，让流可以重新读取
    }

    /**
     * 重写方法
     */
    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val data = requestData(off + len - 1)
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
    }

}