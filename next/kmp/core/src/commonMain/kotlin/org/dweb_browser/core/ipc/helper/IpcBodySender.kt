package org.dweb_browser.core.ipc.helper

import io.ktor.util.cio.toByteArray
import io.ktor.utils.io.cancel
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.ipc.Ipc
import org.dweb_browser.helper.AsyncSetter
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.WeakHashMap
import org.dweb_browser.helper.base64Binary
import org.dweb_browser.helper.collectIn
import org.dweb_browser.helper.consumeEachArrayRange
import org.dweb_browser.helper.getOrPut
import org.dweb_browser.helper.globalDefaultScope
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureBinary
import org.dweb_browser.pure.http.PureBinaryBody
import org.dweb_browser.pure.http.PureEmptyBody
import org.dweb_browser.pure.http.PureStream
import org.dweb_browser.pure.http.PureStreamBody
import org.dweb_browser.pure.http.PureString
import org.dweb_browser.pure.http.PureStringBody

val debugIpcBodySender = Debugger("ipc-body-sender")

/***
 * metaBody 提供真正的数据内存地址
 * IPureBody 提供PureBody的四种类型转换,来构造metaBody
 */

class IpcBodySender private constructor(
  metaBody: MetaBody?,
  override val raw: IPureBody,
  val ipc: Ipc,
) : IpcBody() {

  override lateinit var metaBody: MetaBody

  private val closeSignal = SimpleSignal()
  private val openSignal = SimpleSignal()
  private val _streamOpened = AsyncSetter(false) {
    openSignal.emitAndClear()
  }
  private val _streamClosed = AsyncSetter(false) {
    closeSignal.emitAndClear()
  }
  private val _isStreamOpened by _streamOpened
  private val _isStreamClosed by _streamClosed
  val onStreamClose = closeSignal.toListener()
  val onStreamOpen = openSignal.toListener()
  val isStream by lazy { raw is PureStreamBody }
  val isStreamClosed get() = if (isStream) _isStreamClosed else true
  val isStreamOpened get() = if (isStream) _isStreamOpened else true

  /**
   * 控制信号
   */
  enum class StreamCtorSignal {
    PULLING, PAUSED, ABORTED,
  }

  private val streamCtorSignal = MutableSharedFlow<StreamCtorSignal>()

  init {
    // 如果没有调用from,那么需要自动初始化MetaBody
    if (metaBody != null) {
      initMetaBody(metaBody)
    }
  }

  companion object {
    suspend fun from(raw: IPureBody, ipc: Ipc): IpcBodySender {
      val metaBody = when (raw) {
        is PureEmptyBody -> MetaBody.fromText(raw.toPureString(), ipc.pool.poolId)
        is PureStringBody -> MetaBody.fromText(raw.toPureString(), ipc.pool.poolId)
        is PureBinaryBody -> MetaBody.fromBinary(raw.toPureBinary(), ipc.pool.poolId)
        is PureStreamBody -> null
      }
      return IpcBodySender(
        metaBody, raw, ipc
      ).also {
        if (metaBody == null) {
          it.initMetaBody(it.streamAsMeta(raw.toPureStream()))
        }
      }
    }

    fun fromText(raw: PureString, ipc: Ipc) =
      IpcBodySender(MetaBody.fromText(raw, ipc.pool.poolId), IPureBody.from(raw), ipc)

    fun fromBase64(raw: PureString, ipc: Ipc) = fromBinary(raw.base64Binary, ipc)

    fun fromBinary(raw: PureBinary, ipc: Ipc): IpcBodySender {
      val pureBody = IPureBody.from(raw)

      return IpcBodySender(
        when (pureBody) {
          is PureEmptyBody -> MetaBody.fromText("", ipc.pool.poolId)
          is PureBinaryBody -> MetaBody.fromBinary(pureBody.data, ipc.pool.poolId)
          else -> throw Exception("should not happen")
        }, pureBody, ipc
      )
    }

    suspend fun fromStream(raw: PureStream, ipc: Ipc) = from(IPureBody.from(raw), ipc)

    // 记录PureStream的StreamId
    private val streamIdWM by lazy { WeakHashMap<PureStream, String>() }

    private var stream_id_acc by SafeInt(1)
    private val env = randomUUID().hashCode().toString(36)
    private fun getStreamId(stream: PureStream): String = streamIdWM.getOrPut(stream) {
      "$env-${stream_id_acc++}-§"
    }

    private val asMateLock = Mutex()
  }

  private fun initMetaBody(metaBody: MetaBody) {
    this.metaBody = metaBody
  }

  // 带宽,当前作用不大，当速率有要求的时候才是发挥作用的时候
  private var bandwidth: Int = 0

  // 保险丝
  private var fuse: Int = 0


  /** 拉取数据*/
  private suspend fun emitStreamPull(message: IpcStreamPulling) {
    bandwidth = message.bandwidth
    streamCtorSignal.emit(StreamCtorSignal.PULLING)
  }

  /** 暂停数据*/
  private suspend fun emitStreamPaused(message: IpcStreamPaused) {
    /// 更新带宽负数表示暂停
    bandwidth = -1
    // 更新保险丝设置，用来让底层消化一些消息
    fuse = message.fuse
    streamCtorSignal.emit(StreamCtorSignal.PAUSED)
  }

  /** 解绑使用*/
  private suspend fun emitStreamAborted() {
    streamCtorSignal.emit(StreamCtorSignal.ABORTED)
  }

  /**开始监听控制信号拉取*/
  private fun onPulling(streamId: String) {
    // 接收流的控制信号,才能跟接收者(IpcBodyReceiver)互相配合 onStream = pull + pause + abort
    ipc.onStream("onPulling").collectIn(ipc.scope) { event ->
      val stream = event.data
      if (streamId == stream.stream_id) {
        debugIpcBodySender("onPulling", "sender INIT => $streamId => $stream")
        when (stream) {
          is IpcStreamPulling -> emitStreamPull(stream)
          is IpcStreamPaused -> emitStreamPaused(stream)
          is IpcStreamAbort -> {
            emitStreamAborted()
            ipc.close()
          }

          else -> return@collectIn
        }
        event.consume()
      }
    }
  }

  private suspend fun streamAsMeta(stream: PureStream) = asMateLock.withLock {
    val streamId = getStreamId(stream)
    // 注册数据拉取
    onPulling(streamId)
    debugIpcBodySender("streamAsMeta") { "sender INIT => $streamId => $stream" }
    val streamAsMetaScope = globalDefaultScope + CoroutineName("sender/$stream/$streamId")
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
      reader.consumeEachArrayRange { byteArray, _ ->
        // 保险丝如果不等于0，将会允许一次保险丝销毁
        if (fuse != 0) {
          fuse -= 1
        } else {
          // 等待暂停状态释放
          pullingLock.await()
        }

//        debugIpcBodySender("sender/PULLING/$stream", streamId)
//
//        debugIpcBodySender(
//          "sender/READ/$stream", "${byteArray.size} >> $streamId"
//        )
        //发送流数据
        val message = IpcStreamData.fromBinary(streamId, byteArray)
        ipc.postMessage(message)
        // 发送完成，并将 sendingLock 解锁
//        debugIpcBodySender("sender/PULL-END/$stream", streamId)
      }

      /// END
      debugIpcBodySender(
        "sender/END/$stream", streamId
      )
      /// 不论是不是被 aborted，都发送结束信号
      val message = IpcStreamEnd(streamId)
      ipc.postMessage(message)
      reader.cancel()
      emitStreamClose()
    }

    // 写入第一帧数据 postMessage StreamMain
    var streamType = MetaBody.IPC_META_BODY_TYPE.STREAM_ID
    var streamFirstData: Any = ""
    if (stream is PreReadableInputStream && stream.preReadableSize > 0) {
      streamFirstData = reader.toByteArray(stream.preReadableSize)
      streamType = MetaBody.IPC_META_BODY_TYPE.STREAM_WITH_BINARY
    }

    MetaBody(
      type = streamType,
      data = streamFirstData,
      streamId = streamId,
      senderPoolId = ipc.pool.poolId,
    ).also { metaBody ->
      metaBody.streamId?.let { streamId ->
        // 流对象，写入缓存,用于IpcReceiver快速拿到句柄
        CACHE.streamId_ipcBodySender_Map[streamId] = this
        // 注册销毁时间
        streamAsMetaScope.launch {
          streamCtorSignal.collect { signal ->
            if (signal == StreamCtorSignal.ABORTED) {
              CACHE.streamId_ipcBodySender_Map.remove(streamId)
            }
          }
        }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private suspend inline fun emitStreamClose() {
    if (_isStreamClosed) return
    _streamOpened.set(true)
    _streamClosed.set(true)
    streamCtorSignal.resetReplayCache()
  }

}

