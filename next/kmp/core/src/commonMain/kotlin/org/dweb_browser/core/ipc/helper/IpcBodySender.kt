package org.dweb_browser.core.ipc.helper

import io.ktor.util.cio.toByteArray
import io.ktor.utils.io.cancel
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.AsyncSetter
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.debugger
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printError
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.toBase64ByteArray
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureBinary
import org.dweb_browser.pure.http.PureBinaryBody
import org.dweb_browser.pure.http.PureEmptyBody
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureString
import org.dweb_browser.pure.http.PureStringBody

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
class IpcBodySender private constructor(
  metaBody: MetaBody?,
  override val raw: IPureBody,
  ipc: Ipc,
) : IpcBody() {
  override lateinit var metaBody: MetaBody;

  val isStream by lazy { raw is PureStreamBody }
  val isStreamClosed get() = if (isStream) _isStreamClosed else true
  val isStreamOpened get() = if (isStream) _isStreamOpened else true

//    private val pullSignal = SimpleSignal()
//    private val abortSignal = SimpleSignal()

  /**
   * 控制信号
   */
  enum class StreamCtorSignal {
    PULLING, PAUSED, ABORTED,
  }

  private val streamCtorSignal = MutableSharedFlow<StreamCtorSignal>()

  class IPC {
    companion object {

      /**
       * 某个 IPC 它所能读取的 ipcBody
       */
      class UsableIpcBodyMapper {
        private val map = SafeHashMap</*streamId*/String, IpcBodySender>()

        fun add(streamId: String, ipcBody: IpcBodySender): Boolean = map.sync {
          if (contains(streamId)) {
            false
          } else {
            set(streamId, ipcBody)
            true
          }
        }

        fun get(streamId: String) = map[streamId]
        suspend fun remove(streamId: String) = map.remove(streamId)?.also {
          /// 如果都删除完了，那么就触发事件解绑
          if (map.isEmpty()) {
            this.destroySignal.emitAndClear()
          }
        }

        private val destroySignal = SimpleSignal()
        val onDestroy = destroySignal.toListener()
      }

      private val IpcUsableIpcBodyMap = WeakHashMap<Ipc, UsableIpcBodyMapper>()
      private fun Ipc.getUsableIpcBodyMap(): UsableIpcBodyMapper =
        IpcUsableIpcBodyMap.getOrPut(this) {
          val ipc = this
          debugIpcBody("SenderUsableByIpc", "OPEN => $this")
          UsableIpcBodyMapper().also { mapper ->
            onStream { (message) ->
              when (message) {
                is IpcStreamPulling -> mapper.get(message.stream_id)?.useByIpc(ipc)
                  ?.emitStreamPull(message)

                is IpcStreamPaused -> mapper.get(message.stream_id)?.useByIpc(ipc)
                  ?.emitStreamPaused(message)

                is IpcStreamAbort -> mapper.get(message.stream_id)?.useByIpc(ipc)
                  ?.emitStreamAborted()

                else -> {}
              }
            }.removeWhen(mapper.onDestroy)
            mapper.onDestroy {
              debugIpcBody("SenderUsableByIpc", "CLOSE => $this")
              IpcUsableIpcBodyMap.remove(ipc)
            }
          }
        }

      /**
       * ipc 将会使用 ipcBody
       * 那么只要这个 ipc 接收到 pull 指令，就意味着成为"使用者"，那么这个 ipcBody 都会开始读取数据出来发送
       * 在开始发送第一帧数据之前，其它 ipc 也可以通过 pull 指令来参与成为"使用者"
       */
      fun usableByIpc(ipc: Ipc, ipcBody: IpcBodySender) {
        if (!ipcBody.isStream || ipcBody._isStreamOpened) {
          return
        }

        val streamId = ipcBody.metaBody.streamId!!
        val usableIpcBodyMapper = ipc.getUsableIpcBodyMap()
        if (usableIpcBodyMapper.add(streamId, ipcBody)) {
          // 一个流一旦关闭，那么就将不再会与它有主动通讯上的可能
          ipcBody.onStreamClose {
            usableIpcBodyMapper.remove(streamId)
          }
        }
      }
    }
  }

  /**
   * 被哪些 ipc 所真正使用，以及它们对应的信息
   */
  private val usedIpcMap = mutableMapOf<Ipc, UsedIpcInfo>()

  inner class UsedIpcInfo(
    val ipcBody: IpcBodySender, val ipc: Ipc, var bandwidth: Int = 0, var fuse: Int = 0
  ) {
    suspend fun emitStreamPull(message: IpcStreamPulling) = ipcBody.emitStreamPull(this, message)

    suspend fun emitStreamPaused(message: IpcStreamPaused) = ipcBody.emitStreamPaused(this, message)

    suspend fun emitStreamAborted() = ipcBody.emitStreamAborted(this)
  }


  /**
   * 绑定使用
   * ipc 将使用这个 body，也就是说接下来的 MessageData 也要通知一份给这个 ipc
   * 但一个流一旦开启了，那么就无法再被外部使用了
   */
  private fun useByIpc(ipc: Ipc) = usedIpcMap[ipc] ?: if (isStream) {
    if (!_isStreamOpened) {
      /// 如果是未开启的流，插入
      UsedIpcInfo(this, ipc).also { info ->
        usedIpcMap[ipc] = info
        closeSignal.listen {
          emitStreamAborted(info)
        }
      }
    } else {
      printError("useByIpc", "should not happend");
      debugger()
      null
    }
  } else null

  /**
   * 拉取数据
   */
  private suspend fun emitStreamPull(info: UsedIpcInfo, message: IpcStreamPulling) {
    /// 更新带宽限制
    info.bandwidth = message.bandwidth
    // 只要有一个开始读取，那么就可以开始
    streamCtorSignal.emit(StreamCtorSignal.PULLING)
  }

  /**
   * 暂停数据
   */
  private suspend fun emitStreamPaused(info: UsedIpcInfo, message: IpcStreamPaused) {
    /// 更新保险限制
    info.bandwidth = -1
    info.fuse = message.fuse

    /// 如果所有的读取者都暂停了，那么就触发暂停
    var paused = true
    for (value in usedIpcMap.values) {
      if (value.bandwidth >= 0) {
        paused = false
        break
      }
    }
    if (paused) {
      streamCtorSignal.emit(StreamCtorSignal.PAUSED)
    }
  }

  /**
   * 解绑使用
   */
  private suspend fun emitStreamAborted(info: UsedIpcInfo) {
    if (usedIpcMap.remove(info.ipc) != null) {
      /// 如果没有任何消费者了，那么真正意义上触发 abort
      if (usedIpcMap.isEmpty()) {
        streamCtorSignal.emit(StreamCtorSignal.ABORTED)
      }
    }
  }

  private val closeSignal = SimpleSignal()
  fun onStreamClose(cb: SimpleCallback) = closeSignal.listen(cb)

  private val openSignal = SimpleSignal()
  fun onStreamOpen(cb: SimpleCallback) = openSignal.listen(cb)

  private val _streamOpened = AsyncSetter(false) {
    openSignal.emitAndClear()
  }

  private val _isStreamOpened by _streamOpened

  private val _streamClosed = AsyncSetter(false) {
    closeSignal.emitAndClear()
  }
  private val _isStreamClosed by _streamClosed

  private suspend inline fun emitStreamClose() {
    if (_isStreamClosed) return
    _streamOpened.set(true)
    _streamClosed.set(true)
    streamCtorSignal.resetReplayCache()
  }


//  override val metaBody: MetaBody


  init {
    if (metaBody != null) {
      this.metaBody = metaBody
    }
//    metaBody = runBlockingCatching { bodyAsMeta(raw, ipc) }.getOrThrow()

    /// 作为 "生产者"，第一持有这个 IpcBodySender
    IPC.usableByIpc(ipc, this)
  }

  companion object {
    suspend fun from(raw: IPureBody, ipc: Ipc): IpcBodySender {
      val metaBody = when (raw) {
        is PureEmptyBody -> MetaBody.fromText(ipc.uid, raw.toPureString())
        is PureStringBody -> MetaBody.fromText(ipc.uid, raw.toPureString())
        is PureBinaryBody -> MetaBody.fromBinary(ipc, raw.toPureBinary())
        is PureStreamBody -> null
      }
      return IpcBodySender(
        metaBody, raw, ipc
      ).also {
        if (metaBody == null) {
          it.metaBody = it.streamAsMeta(raw.toPureStream(), ipc)
        }
      }
    }


    fun fromText(raw: PureString, ipc: Ipc) =
      IpcBodySender(MetaBody.fromText(ipc.uid, raw), IPureBody.from(raw), ipc)

    fun fromBase64(raw: PureString, ipc: Ipc) = fromBinary(raw.toBase64ByteArray(), ipc)

    fun fromBinary(raw: PureBinary, ipc: Ipc): IpcBodySender {
      val pureBody = IPureBody.from(raw)

      return IpcBodySender(
        when (pureBody) {
          is PureEmptyBody -> MetaBody.fromText(ipc.uid, "")
          is PureBinaryBody -> MetaBody.fromBinary(ipc.uid, pureBody.data)
          else -> throw Exception("should not happen")
        },
        pureBody,
        ipc
      )
    }

    suspend fun fromStream(raw: PureStream, ipc: Ipc) = from(IPureBody.from(raw), ipc)


    private val streamIdWM by lazy { WeakHashMap<PureStream, String>() }

    private var stream_id_acc by SafeInt(1);
    private val env = randomUUID()
    private fun getStreamId(stream: PureStream): String = streamIdWM.getOrPut(stream) {
      "rs-$env-${stream_id_acc++}"
    };

    /**
     * 任意的 RAW 背后都会有一个 IpcBodySender/IpcBodyReceiver
     * 将它们缓存起来，那么使用这些 RAW 确保只拿到同一个 IpcBody，这对 RAW-Stream 很重要，流不可以被多次打开读取
     */
    private val asMatedStreams = SafeHashMap<PureStream, MetaBody>()
    private val asMateLock = Mutex()
  }


  private suspend fun bodyAsMeta(body: IPureBody, ipc: Ipc) = when (body) {
    is PureEmptyBody -> MetaBody.fromText(ipc.uid, body.toPureString())
    is PureStringBody -> MetaBody.fromText(ipc.uid, body.toPureString())
    is PureBinaryBody -> MetaBody.fromBinary(ipc, body.toPureBinary())
    is PureStreamBody -> streamAsMeta(body.toPureStream(), ipc)
  }


  private suspend fun streamAsMeta(stream: PureStream, ipc: Ipc) = asMateLock.withLock {
    val stream_id = getStreamId(stream)
    debugIpcBody("streamAsMeta", "sender INIT => $stream_id => $stream")
    val streamAsMetaScope =
      CoroutineScope(CoroutineName("sender/$stream/$stream_id") + ioAsyncExceptionHandler)
    val reader by lazy { stream.getReader("ipcBodySender StreamAsMeta") }
    streamAsMetaScope.launch {
      /**
       * 流的使用锁(Future 锁)
       * 只有等到 Pulling 指令的时候才能读取并发送
       */
      var pullingLock = CompletableDeferred<Unit>()
      val pullingObj = SynchronizedObject()
      launch {
        streamCtorSignal.collect { signal ->
          when (signal) {
            StreamCtorSignal.PULLING -> synchronized(pullingObj) {
              pullingLock.complete(Unit)
            }

            StreamCtorSignal.PAUSED -> synchronized(pullingObj) {
              if (pullingLock.isCompleted) {// 如果已经被解锁了，那么重新生成一个锁
                pullingLock = CompletableDeferred()
              }
            }

            StreamCtorSignal.ABORTED -> {
              reader.cancel()
              emitStreamClose()
            }
          }
        }
      }

      // 等待流开始被拉取
      pullingLock.await()
      /// READ-ALL 持续发送数据
      reader.consumeEachArrayRange { byteArray, last ->
        // 等待暂停状态释放
        pullingLock.await()

        debugIpcBody("sender/PULLING/$stream", stream_id)

        // 开光了，流已经开始被读取
        _streamOpened.set(true)
        debugIpcBody(
          "sender/READ/$stream", "${byteArray.size} >> $stream_id"
        )
        val message = IpcStreamData.fromBinary(stream_id, byteArray)
        for (usedIpc in usedIpcMap.keys) {
          usedIpc.postMessage(message)
        }

        // 发送完成，并将 sendingLock 解锁
        debugIpcBody("sender/PULL-END/$stream", stream_id)
      }

      /// END
      debugIpcBody(
        "sender/END/$stream", stream_id
      )
      /// 不论是不是被 aborted，都发送结束信号
      val message = IpcStreamEnd(stream_id)
      for (usedIpc in usedIpcMap.keys) {
        usedIpc.postMessage(message)
      }
      reader.cancel()
      emitStreamClose()
    }

    // 写入第一帧数据
    var streamType = MetaBody.IPC_META_BODY_TYPE.STREAM_ID
    var streamFirstData: Any = ""
    if (stream is PreReadableInputStream && stream.preReadableSize > 0) {
      streamFirstData = reader.toByteArray(stream.preReadableSize)
      streamType = MetaBody.IPC_META_BODY_TYPE.STREAM_WITH_BINARY
    }

    MetaBody(
      type = streamType, senderUid = ipc.uid, data = streamFirstData, streamId = stream_id
    ).also { metaBody ->
      // 流对象，写入缓存
      CACHE.metaId_ipcBodySender_Map[metaBody.metaId] = this
      streamAsMetaScope.launch {
        streamCtorSignal.collect { signal ->
          if (signal == StreamCtorSignal.ABORTED) {
            CACHE.metaId_ipcBodySender_Map.remove(metaBody.metaId)
          }
        }
      }
    }
  }
}

