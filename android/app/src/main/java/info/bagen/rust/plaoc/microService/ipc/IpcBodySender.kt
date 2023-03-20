package info.bagen.rust.plaoc.microService.ipc

import info.bagen.rust.plaoc.microService.helper.*
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * IpcBodySender 本质上是对 ReadableStream 的再次封装。
 * 我们知道 ReadableStream 本质上是由 stream 与 controller 组成。二者分别代表着 reader 与 writer 两个角色。
 *
 * 而 IpcBodySender 则是将 controller 给一个 ipc 来做写入，将 stream 给另一个 ipc 来做接收。
 * 而关键点就在于这两个 ipc 很可能不是对等关系
 *
 * 因为 IpcBodySender 会被 IpcRequest/http4kRequest、IpcResponse/http4kResponse 对象转换的时候传递，
 * 中间被很多个 ipc 所持有过，而每一个持有过它的人都有可能是这个 stream 的读取者。
 *
 * 因此我们定义了两个集合，一个是 ipc 的 usableIpcBodyMap；一个是 ipcBodySender 这边的 usedIpcMap
 *
 */
class IpcBodySender(
    override val raw: Any,
    ipc: Ipc,
) : IpcBody() {
    val isStream by lazy { raw is InputStream }
    val isStreamClosed get() = if (isStream) _isStreamClosed else true
    val isStreamOpened get() = if (isStream) _isStreamOpened else true

    private val pullSignal = SimpleSignal()
    private val abortSignal = SimpleSignal()

    class IPC {
        companion object {

            data class UsableIpcBodyMapper(

                private val map: MutableMap<String, IpcBodySender> = mutableMapOf()
            ) {
                fun add(streamId: String, ipcBody: IpcBodySender): Boolean {
                    if (map.contains(streamId)) {
                        return false
                    }
                    map[streamId] = ipcBody
                    return true
                }

                fun get(streamId: String) = map[streamId]
                suspend fun remove(streamId: String) = map.remove(streamId)?.also {
                    /// 如果都删除完了，那么就触发事件解绑
                    if (map.isEmpty()) {
                        this.destroySignal.emit(Unit)
                        this.destroySignal.clear()
                    }
                }

                private val destroySignal = SimpleSignal()
                fun onDestroy(cb: SimpleCallback) = destroySignal.listen(cb)
            }

            private val IpcUsableIpcBodyMap = WeakHashMap<Ipc, UsableIpcBodyMapper>()

            /**
             * ipc 将会使用 ipcBody
             * 那么只要这个 ipc 接收到 pull 指令，就意味着成为"使用者"，那么这个 ipcBody 都会开始读取数据出来发送
             * 在开始发送第一帧数据之前，其它 ipc 也可以通过 pull 指令来参与成为"使用者"
             */
            fun usableByIpc(ipc: Ipc, ipcBody: IpcBodySender) {
                if (ipcBody.isStream && !ipcBody._isStreamOpened) {
                    val streamId = ipcBody.metaBody.streamId!!
                    val usableIpcBodyMapper = IpcUsableIpcBodyMap.getOrPut(ipc) {
                        debugIpcBody("ipcBodySenderUsableByIpc/OPEN/$ipc")
                        UsableIpcBodyMapper().also { mapper ->
                            val off = ipc.onMessage { (message) ->
                                when (message) {
                                    is IpcStreamPull -> {
                                        mapper.get(message.stream_id)?.also { ipcBody ->
                                            // 一个流一旦开启了，那么就无法再被外部使用了
                                            if (ipcBody.useByIpc(ipc)) { // ipc 将使用这个 body，也就是说接下来的 MessageData 也要通知一份给这个 ipc
                                                ipcBody.emitStreamPull(message, ipc)
                                            }
                                        }
                                    }
                                    is IpcStreamAbort -> {
                                        // 一个流一旦开启了，那么就无法再被外部使用了
                                        mapper.get(message.stream_id)?.also { ipcBody ->
                                            ipcBody.unuseByIpc(ipc)
                                        }
                                    }
                                    else -> {}
                                }
                            }
                            mapper.onDestroy(off)
                            mapper.onDestroy {
                                debugIpcBody("ipcBodySenderUsableByIpc/CLOSE/$ipc")
                                IpcUsableIpcBodyMap.remove(ipc)
                            }
                        }
                    }
                    if (usableIpcBodyMapper.add(streamId, ipcBody)) {
                        // 一个流一旦关闭，那么就将不再会与它有主动通讯上的可能
                        ipcBody.onStreamClose {
                            usableIpcBodyMapper.remove(streamId)
                        }
                    }

                }
            }
        }
    }

    /**
     * 被哪些 ipc 所真正使用，使用的进度分别是多少
     *
     * 这个进度 用于 类似流的 多发
     */
    private val usedIpcMap = mutableMapOf<Ipc, /* desiredSize */ Int>()


    /**
     * 绑定使用
     */
    private fun useByIpc(ipc: Ipc): Boolean {
        if (usedIpcMap.contains(ipc)) {
            return true
        }
        if (isStream && !_isStreamOpened) {
            usedIpcMap[ipc] = 0
            closeSignal.listen {
                unuseByIpc(ipc)
            }
            return true
        }
        return false
    }

    /**
     * 拉取数据
     */
    private suspend fun emitStreamPull(message: IpcStreamPull, ipc: Ipc) {
        /// desiredSize 仅作参考，我们以发过来的拉取次数为准
        val pulledSize = usedIpcMap[ipc]!! + message.desiredSize
        usedIpcMap[ipc] = pulledSize
        pullSignal.emit(Unit)
    }

    /**
     * 解绑使用
     */
    private suspend fun unuseByIpc(ipc: Ipc) {
        if (usedIpcMap.remove(ipc) != null) {
            /// 如果没有任何消费者了，那么真正意义上触发 abort
            if (usedIpcMap.isEmpty()) {
                abortSignal.emit(Unit)
            }
        }
    }

    private val closeSignal = SimpleSignal()
    fun onStreamClose(cb: SimpleCallback) = closeSignal.listen(cb)

    private val openSignal = SimpleSignal()
    fun onStreamOpen(cb: SimpleCallback) = openSignal.listen(cb)

    private var _isStreamOpened = false
        set(value) {
            if (field != value) {
                field = value
                runBlockingCatching {
                    openSignal.emit(Unit)
                    openSignal.clear()
                }.getOrThrow()
            }
        }
    private var _isStreamClosed = false
        set(value) {
            if (field != value) {
                field = value
                runBlockingCatching {
                    closeSignal.emit(Unit)
                    closeSignal.clear()
                }.getOrThrow()
            }
        }

    private inline fun emitStreamClose() {
        _isStreamOpened = true
        _isStreamClosed = true
    }


    override val metaBody = bodyAsMeta(raw, ipc)
    override val bodyHub by lazy {
        BodyHub().also {
            it.data = raw
            when (raw) {
                is String -> it.text = raw;
                is ByteArray -> it.u8a = raw
                is InputStream -> it.stream = raw
            }
        }
    }


    init {
        CACHE.raw_ipcBody_WMap[raw] = this

        /// 作为 "生产者"，第一持有这个 IpcBodySender
        IPC.usableByIpc(ipc, this)
    }

    companion object {

        fun from(raw: Any, ipc: Ipc) = CACHE.raw_ipcBody_WMap[raw] ?: IpcBodySender(raw, ipc)


        private val streamIdWM by lazy { WeakHashMap<InputStream, String>() }

        private var stream_id_acc = AtomicInteger(1);
        private fun getStreamId(stream: InputStream): String = streamIdWM.getOrPut(stream) {
            if (stream is ReadableStream) {
                "rs-${stream_id_acc.getAndAdd(1)}[${stream.uid}]"
            } else {
                "rs-${stream_id_acc.getAndAdd(1)}"
            }
        };

    }


    private fun bodyAsMeta(body: Any, ipc: Ipc) = when (body) {
        is String -> MetaBody.fromText(ipc.uid, body)
        is ByteArray -> MetaBody.fromBinary(ipc, body)
        is InputStream -> streamAsMeta(body, ipc)
        else -> throw Exception("invalid body type $body")
    }

    private fun streamAsMeta(stream: InputStream, ipc: Ipc): MetaBody {

        val stream_id = getStreamId(stream)
        debugIpcBody("sender/INIT/$stream", stream_id)
        val streamAsMetaScope =
            CoroutineScope(CoroutineName("sender/$stream/$stream_id") + ioAsyncExceptionHandler)

        /**
         * 发送锁
         *
         * 流是不可以被并发读取的
         */
        val sendingLock = AtomicBoolean()
        suspend fun sender() {
            // 上锁，如果已经有锁，那说明已经在被读取了，直接退出
            if (sendingLock.getAndSet(true)) {
                println("DOUBLE PULL!!!")
                return
            }

            debugIpcBody("sender/PULLING/$stream", stream_id)
            when (val availableLen = stream.available()) {
                -1, 0 -> {
                    debugIpcBody(
                        "sender/END/$stream", "$availableLen >> $stream_id"
                    )
                    /// 不论是不是被 aborted，都发送结束信号
                    val message = IpcStreamEnd(stream_id)
                    for (ipc in usedIpcMap.keys) {
                        ipc.postMessage(message)
                    }

                    emitStreamClose()
                }
                else -> {
                    // 开光了，流已经开始被读取
                    _isStreamOpened = true
                    debugIpcBody(
                        "sender/READ/$stream", "$availableLen >> $stream_id"
                    )
                    val message =
                        IpcStreamData.fromBinary(stream_id, stream.readByteArray(availableLen))
                    for (ipc in usedIpcMap.keys) {
                        ipc.postMessage(message)
                    }
                }
            }
            // 发送完成，解锁
            sendingLock.set(false)
            debugIpcBody("sender/PULL-END/$stream", stream_id)
        }
        pullSignal.listen {
            streamAsMetaScope.launch {
                sender()
            }
        }
        abortSignal.listen {
            stream.close()
            emitStreamClose()
        }

        // 写入第一帧数据
        var streamType = MetaBody.IPC_META_BODY_TYPE.STREAM_ID
        var streamFirstData: Any = ""
        if (stream is PreReadableInputStream && stream.preReadableSize > 0) {
            streamFirstData = stream.readByteArray(stream.preReadableSize)
            streamType = MetaBody.IPC_META_BODY_TYPE.STREAM_WITH_BINARY
        }

        return MetaBody(
            type = streamType,
            senderUid = ipc.uid,
            data = streamFirstData,
            streamId = stream_id
        ).also { metaBody ->
            // 流对象，写入缓存
            CACHE.metaId_ipcBodySender_Map[metaBody.metaId] = this
            abortSignal.listen {
                CACHE.metaId_ipcBodySender_Map.remove(metaBody.metaId)
            }
        }
    }
}


/**
 * 可预读取的流
 */
interface PreReadableInputStream {
    /**
     * 对标 InputStream.available 函数
     * 返回可预读的数据
     */
    val preReadableSize: Int
}