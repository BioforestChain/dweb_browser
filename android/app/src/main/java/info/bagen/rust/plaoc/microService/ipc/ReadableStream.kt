package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.helper.Signal
import info.bagen.rust.plaoc.microService.helper.printdebugln
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.InputStream

inline fun debugStream(tag: String, msg: Any = "", err: Throwable? = null) =
    printdebugln("stream", tag, msg, err)

/**
 * 模拟Web的 ReadableStream
 */
class ReadableStream(
    val onStart: suspend (arg: ReadableStreamController) -> Unit = {},
    val onPull: suspend (arg: Pair<Int, ReadableStreamController>) -> Unit = {}
) : InputStream() {

    // 数据源
    private var _data: ByteArray = byteArrayOf()
    private var ptr = 0 // 当前指针
    private var mark = 0 //标记

    class ReadableStreamController(
        private val dataChannel: Channel<ByteArray>,
        val getStream: () -> ReadableStream
    ) {
        val stream get() = getStream()

        suspend fun enqueue(byteArray: ByteArray) =
            dataChannel.send(byteArray)

        fun close() {
            dataChannel.close()
        }

        fun error(e: Throwable?) = dataChannel.close(e)
    }


    private val dataChannel = Channel<ByteArray>()
    private val pullSignal = Signal<Int>()

    private val controller by lazy { ReadableStreamController(dataChannel) { this@ReadableStream } }

    private val writeDataScope =
        CoroutineScope(CoroutineName("readableStream/writeData") + Dispatchers.IO)
    private val readDataScope =
        CoroutineScope(CoroutineName("readableStream/readData") + Dispatchers.IO)

    init {
        runBlocking {
            onStart(controller)
        }
        pullSignal.listen { desiredSize ->
            debugStream("PULL/START/${uid}", desiredSize)
            onPull(Pair(desiredSize, controller))
            debugStream("PULL/END/${uid}", desiredSize)
        }
        writeDataScope.launch {
            // 一直等待数据
            for (chunk in dataChannel) {
                _data += chunk
                debugStream("DATA-IN/$uid", "+${chunk.size} ~> ${_data.size}")
                // 收到数据了，尝试解锁通知等待者
                dataSizeState.emit(_data.size)
            }
            // 关闭数据通道了，尝试解锁通知等待者
            dataSizeState.emit(-1)
            closePo.resolve(Unit)
        }
    }

    private val closePo = PromiseOut<Unit>()

    //    private val dataSizeChangeChannel = Channel<Int>()
    private val dataSizeState = MutableStateFlow(_data.size)
    private val dataSizeFlow = dataSizeState.asSharedFlow()

    suspend fun afterClosed() {
        closePo.waitPromise()
    }

    val isClosed get() = closePo.isFinished


    /**
     * 读取数据，在尽可能满足下标读取的情况下
     */
    private fun requestData(ptr: Int): ByteArray {
        // 如果下标满足条件，直接返回
        if (ptr < _data.size) {
            return _data
        }

        runBlocking {
            val desiredSize = ptr + 1
            writeDataScope.async {
                pullSignal.emit(desiredSize)
            }.join()
            readDataScope.async {
                val wait = PromiseOut<Unit>()
                val c = launch {
                    dataSizeFlow.collect { size ->
                        debugStream("REQUEST-DATA/CHANGED/$uid", "$size/$desiredSize")
                        when {
                            size == -1 -> wait.resolve(Unit) // 不需要抛出错误
                            ptr < size -> wait.resolve(Unit)
                        }
                    }
                }
                wait.waitPromise()
                c.cancel()
                debugStream("REQUEST-DATA/OK/$uid", _data.size)
            }.join()
        }

        // 控制端无法读取数据了，只能直接返回
        return _data
    }

    companion object {
        private var id_acc = 0
    }

    private val uid = "#s${id_acc++}"
    override fun toString() = uid


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
        debugStream("CLOSE/${uid}")
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