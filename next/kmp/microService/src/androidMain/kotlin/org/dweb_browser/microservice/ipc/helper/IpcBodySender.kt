package org.dweb_browser.microservice.ipc.helper

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.dweb_browser.helper.SimpleCallback
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.debugger
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.printError
import org.dweb_browser.helper.readByteArray
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.ipc.Ipc
import java.io.InputStream
import java.util.WeakHashMap
import java.util.concurrent.CompletableFuture
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
      data class UsableIpcBodyMapper(
        private val map: MutableMap</*streamId*/String, IpcBodySender> = mutableMapOf()
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
          debugIpcBody("ipcBodySenderUsableByIpc/OPEN/$this")
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
              debugIpcBody("ipcBodySenderUsableByIpc/CLOSE/$this")
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
    suspend fun emitStreamPull(message: IpcStreamPulling) =
      ipcBody.emitStreamPull(this, message)

    suspend fun emitStreamPaused(message: IpcStreamPaused) =
      ipcBody.emitStreamPaused(this, message)

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
    for (info in usedIpcMap.values) {
      if (info.bandwidth >= 0) {
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

  private var _isStreamOpened = false
    set(value) {
      if (field != value) {
        field = value
        runBlockingCatching {
          openSignal.emitAndClear()
        }.getOrThrow()
      }
    }
  private var _isStreamClosed = false
    set(value) {
      if (field != value) {
        field = value
        runBlockingCatching {
          closeSignal.emitAndClear()
        }.getOrThrow()
      }
    }

  private inline fun emitStreamClose() {
    if (_isStreamClosed) return
    _isStreamOpened = true
    _isStreamClosed = true
    streamCtorSignal.resetReplayCache()
  }


  override val metaBody = bodyAsMeta(raw, ipc)
  override val bodyHub by lazy {
    BodyHub().also {
      it.data = raw
      when (raw) {
        is String -> it.base64 = raw;
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

    private fun fromAny(raw: Any, ipc: Ipc) =
      CACHE.raw_ipcBody_WMap[raw] ?: IpcBodySender(raw, ipc)

    fun fromText(raw: String, ipc: Ipc) = fromBinary(raw.toByteArray(), ipc)
    fun fromBase64(raw: String, ipc: Ipc) = fromAny(raw, ipc)
    fun fromBinary(raw: ByteArray, ipc: Ipc) = fromAny(raw, ipc)
    fun fromStream(raw: InputStream, ipc: Ipc) = fromAny(raw, ipc)


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
    streamAsMetaScope.launch {
      /**
       * 流的使用锁(Future 锁)
       * 只有等到 Pulling 指令的时候才能读取并发送
       */
      var pullingLock = CompletableFuture<Unit>()
      launch {
        streamCtorSignal.collect { signal ->
          when (signal) {
            StreamCtorSignal.PULLING -> synchronized(pullingLock) {
              pullingLock.complete(Unit)
            }

            StreamCtorSignal.PAUSED -> synchronized(pullingLock) {
              if (pullingLock.isDone) {// 如果已经被解锁了，那么重新生成一个锁
                pullingLock = CompletableFuture()
              }
            }

            StreamCtorSignal.ABORTED -> {
              stream.close()
              emitStreamClose()
            }
          }
        }
      }

      /// 持续发送数据
      while (true) {
        // 等待流开始被拉取
        pullingLock.await()

        debugIpcBody("sender/PULLING/$stream", stream_id)
        when (val availableLen = stream.available()) {
          -1 -> {
            debugIpcBody(
              "sender/END/$stream", "$availableLen >> $stream_id"
            )
            /// 不论是不是被 aborted，都发送结束信号
            val message = IpcStreamEnd(stream_id)
            for (ipc in usedIpcMap.keys) {
              ipc.postMessage(message)
            }
            stream.close()
            emitStreamClose()
            break
          }

          0 -> {
            debugIpcBody("sender/EMPTY/$stream", stream_id)
          }

          else -> {
            // 开光了，流已经开始被读取
            _isStreamOpened = true
            debugIpcBody(
              "sender/READ/$stream", "$availableLen >> $stream_id"
            )
            val message = IpcStreamData.fromBinary(
              stream_id, stream.readByteArray(availableLen)
            )
            for (ipc in usedIpcMap.keys) {
              ipc.postMessage(message)
            }
          }
        }
        // 发送完成，并将 sendingLock 解锁
        debugIpcBody("sender/PULL-END/$stream", stream_id)
      }
    }

    // 写入第一帧数据
    var streamType = MetaBody.IPC_META_BODY_TYPE.STREAM_ID
    var streamFirstData: Any = ""
    if (stream is PreReadableInputStream && stream.preReadableSize > 0) {
      streamFirstData = stream.readByteArray(stream.preReadableSize)
      streamType = MetaBody.IPC_META_BODY_TYPE.STREAM_WITH_BINARY
    }

    return MetaBody(
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

