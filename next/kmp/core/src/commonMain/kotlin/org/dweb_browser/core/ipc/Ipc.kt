package org.dweb_browser.core.ipc

import io.ktor.http.Url
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.IMicroModuleManifest
import org.dweb_browser.core.ipc.helper.IPC_STATE
import org.dweb_browser.core.ipc.helper.IpcClientRequest
import org.dweb_browser.core.ipc.helper.IpcClientRequest.Companion.toIpc
import org.dweb_browser.core.ipc.helper.IpcError
import org.dweb_browser.core.ipc.helper.IpcErrorMessageArgs
import org.dweb_browser.core.ipc.helper.IpcEvent
import org.dweb_browser.core.ipc.helper.IpcEventMessageArgs
import org.dweb_browser.core.ipc.helper.IpcLifeCycle
import org.dweb_browser.core.ipc.helper.IpcLifeCycleMessageArgs
import org.dweb_browser.core.ipc.helper.IpcMessage
import org.dweb_browser.core.ipc.helper.IpcMessageArgs
import org.dweb_browser.core.ipc.helper.IpcRequest
import org.dweb_browser.core.ipc.helper.IpcRequestMessageArgs
import org.dweb_browser.core.ipc.helper.IpcResponse
import org.dweb_browser.core.ipc.helper.IpcResponseMessageArgs
import org.dweb_browser.core.ipc.helper.IpcServerRequest
import org.dweb_browser.core.ipc.helper.IpcStream
import org.dweb_browser.core.ipc.helper.IpcStreamMessageArgs
import org.dweb_browser.core.ipc.helper.OnIpcErrorMessage
import org.dweb_browser.core.ipc.helper.OnIpcEventMessage
import org.dweb_browser.core.ipc.helper.OnIpcLifeCycleMessage
import org.dweb_browser.core.ipc.helper.OnIpcMessage
import org.dweb_browser.core.ipc.helper.OnIpcRequestMessage
import org.dweb_browser.core.ipc.helper.OnIpcResponseMessage
import org.dweb_browser.core.ipc.helper.OnIpcStreamMessage
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SafeInt
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.SimpleSignal
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureHeaders
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureResponse

val debugIpc = Debugger("ipc")

/**
 * 抽象工厂模式
 */
abstract class Ipc(val channelId: String, val endpoint: IpcPool) {
  companion object {
    private var uid_acc by SafeInt(1)
    private var reqId_acc by SafeInt(0)
    var order_by_acc by SafeInt(0)
    private val ipcMessageCoroutineScope =
      CoroutineScope(CoroutineName("ipc-message") + ioAsyncExceptionHandler)
  }

  abstract val remote: IMicroModuleManifest
  fun remoteAsInstance() = if (remote is MicroModule) remote as MicroModule else null


  val uid = uid_acc++
  private val pid = endpoint.generatePid(channelId)

  private var ipcLifeCycleState: IPC_STATE = IPC_STATE.OPENING

  /**-----protocol support start*/
  /**
   * 是否支持 cbor 协议传输：
   * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 cbor 的编解码
   */
  open val supportCbor: Boolean = false

  /**
   * 是否支持 Protobuf 协议传输：
   * 需要同时满足两个条件：通道支持直接传输二进制；通达支持 Protobuf 的编解码
   */
  open val supportProtobuf: Boolean = false

  /**
   * 是否支持结构化内存协议传输：
   * 就是说不需要对数据手动序列化反序列化，可以直接传输内存对象
   */
  open val supportRaw: Boolean = false

  /** 是否支持 二进制 传输 */
  open val supportBinary: Boolean = false // get() = supportCbor || supportProtobuf

  /**-----protocol support end*/
  override fun toString() = "Ipc#state=$ipcLifeCycleState,channelId=$channelId"

  // 发消息
  abstract suspend fun doPostMessage(pid: Int, data: IpcMessage)

  /**发送各类消息到remote*/
  suspend fun postMessage(data: IpcMessage) {
    if (isClosed) {
      debugIpcPool("fail to post message, already closed")
      return
    }
    // 等待通信建立完成（如果通道没有建立完成，并且不是生命周期消息）
    if (!isActivity && data !is IpcLifeCycle) {
      awaitStart()
    }
    println("$channelId sendMessage $data")
    // 分发消息
    this.doPostMessage(this.pid, data)
  }

  private val _messageSignal = Signal<IpcMessageArgs>()
  fun onMessage(cb: OnIpcMessage) = _messageSignal.listen(cb)

  /**分发各类消息到本地*/
  suspend fun emitMessage(args: IpcMessageArgs) = _messageSignal.emit(args)

  /**-----onMessage start*/
  private fun <T : Any> createSignal(): Signal<T> {
    val signal = Signal<T>()
    this.onClose {
      signal.clear()
    }
    return signal
  }

  private val _requestSignal by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    createSignal<IpcRequestMessageArgs>().also { signal ->
      _messageSignal.listen { args ->
        when (val ipcReq = args.message) {
          is IpcRequest -> {
            val ipcServerRequest = when (ipcReq) {
              is IpcClientRequest -> ipcReq.toServer(args.ipc)
              is IpcServerRequest -> ipcReq
            }
            ipcMessageCoroutineScope.launch {
              signal.emit(
                IpcRequestMessageArgs(ipcServerRequest, args.ipc)
              )
            }
          }

          else -> {}
        }
      }
    }
  }

  fun onRequest(cb: OnIpcRequestMessage) = _requestSignal.listen(cb)

  private val _responseSignal by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    createSignal<IpcResponseMessageArgs>().also { signal ->
      _messageSignal.listen { args ->
        if (args.message is IpcResponse) {
          ipcMessageCoroutineScope.launch {
            signal.emit(
              IpcResponseMessageArgs(
                args.message, args.ipc
              )
            )
          }
        }
      }
    }
  }

  fun onResponse(cb: OnIpcResponseMessage) = _responseSignal.listen(cb)

  private val _streamSignal by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    val signal = createSignal<IpcStreamMessageArgs>()
    /// 这里建立起一个独立的顺序队列，目的是避免处理阻塞
    /// TODO 这里不应该使用 UNLIMITED，而是压力到一定程度方向发送限流的指令
    val streamChannel = Channel<IpcStreamMessageArgs>(capacity = Channel.UNLIMITED)
    ipcMessageCoroutineScope.launch {
      for (message in streamChannel) {
        signal.emit(message)
      }
    }
    _messageSignal.listen { args ->
      if (args.message is IpcStream) {
        streamChannel.trySend(
          IpcStreamMessageArgs(
            args.message, args.ipc
          )
        )
      }
    }
    onClose {
      streamChannel.close()
    }
    signal
  }

  fun onStream(cb: OnIpcStreamMessage) = _streamSignal.listen(cb)

  // 根据 StreamId 分发控制消息给当前这个endPoint的各个Body
  fun onPulling(
    streamId: String,
    onPulling: suspend (message: IpcStream, close: () -> Unit) -> Unit
  ) {
    onStream { (message) ->
      if (message.stream_id == streamId) {
        onPulling(message) {
          debugIpc("onPulling", "stream:$streamId listen close")
          offListener()
        }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private val _eventSignal by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    createSignal<IpcEventMessageArgs>().also { signal ->
      val orderByChannels = SafeHashMap<Int, Channel<IpcEventMessageArgs>>()
      _messageSignal.listen { args ->
        if (args.message is IpcEvent) {
          val eventArgs = IpcEventMessageArgs(
            args.message, args.ipc
          )
          when (val orderBy = args.message.orderBy) {
            /// 无序模式
            null -> ipcMessageCoroutineScope.launch {
              signal.emit(eventArgs)
            }
            /// 有序模式
            else -> {
              val orderedEvents = orderByChannels.getOrPut(orderBy) {
                Channel<IpcEventMessageArgs>(capacity = Channel.UNLIMITED).also { events ->
                  var lastEmitTime = 0;
                  /// 进行有序地发送
                  val sendJob = ipcMessageCoroutineScope.launch {
                    for (it in events) {
                      signal.emit(it)
                      lastEmitTime = 10
                    }
                  }
                  val destroyEvents = {
                    sendJob.cancel()

                    orderByChannels.remove(orderBy)
                    events.close()
                  }
                  /// 定时器释放内存
                  val gcJob = ipcMessageCoroutineScope.launch {
                    while (true) {
                      delay(100)
                      lastEmitTime--
                      if (lastEmitTime <= 0 && events.isEmpty) {
                        destroyEvents()
                        break
                      }
                    }
                  }
                  /// ipc 关闭时释放
                  onClose {
                    gcJob.cancel()
                    destroyEvents()
                  }
                }
              }
              orderedEvents.send(eventArgs)
            }
          }
        }
      }
    }
  }


  fun onEvent(cb: OnIpcEventMessage) = _eventSignal.listen(cb)

  private val _lifeCycleSignal by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    createSignal<IpcLifeCycleMessageArgs>().also { signal ->
      _messageSignal.listen { args ->
        if (args.message is IpcLifeCycle) {
          ipcMessageCoroutineScope.launch {
            signal.emit(
              IpcLifeCycleMessageArgs(
                args.message, args.ipc
              )
            )
          }
        }
      }
    }
  }

  fun onLifeCycle(cb: OnIpcLifeCycleMessage) = _lifeCycleSignal.listen(cb)

  private val _errorSignal by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    createSignal<IpcErrorMessageArgs>().also { signal ->
      _messageSignal.listen { args ->
        if (args.message is IpcError) {
          ipcMessageCoroutineScope.launch {
            signal.emit(
              IpcErrorMessageArgs(
                args.message, args.ipc
              )
            )
          }
        }
      }
    }
  }

  fun onError(cb: OnIpcErrorMessage) = _errorSignal.listen(cb)

  /**-----onMessage end*/


  /**----- 发送请求 start */
  suspend fun request(url: String) = request(PureClientRequest(method = PureMethod.GET, href = url))

  suspend fun request(url: Url) =
    request(PureClientRequest(method = PureMethod.GET, href = url.toString()))

  suspend fun postResponse(reqId: Int, response: PureResponse) {
    postMessage(
      IpcResponse.fromResponse(
        reqId, response, this
      )
    )
  }

  private val _reqResMap by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    SafeHashMap<Int, CompletableDeferred<IpcResponse>>().also { reqResMap ->
      onResponse { (response) ->
        val result = reqResMap.remove(response.reqId)
          ?: throw Exception("no found response by reqId: ${response.reqId}")
        result.complete(response)
      }
    }
  }

  suspend fun request(ipcRequest: IpcRequest): IpcResponse {
    val result = CompletableDeferred<IpcResponse>()
    _reqResMap[ipcRequest.reqId] = result
    this.postMessage(ipcRequest)
    return result.await()
  }

  private suspend fun _buildIpcRequest(url: String, init: IpcRequestInit): IpcRequest {
    val reqId = this.allocReqId()
    return IpcClientRequest.fromRequest(reqId, this, url, init)
  }

  // PureClientRequest -> ipcRequest -> IpcResponse -> PureResponse
  suspend fun request(pureRequest: PureClientRequest): PureResponse {
    return this.request(
      pureRequest.toIpc(allocReqId(), this)
    ).toPure()
  }

  suspend fun request(url: String, init: IpcRequestInit): IpcResponse {
    val ipcRequest = this._buildIpcRequest(url, init)
    return request(ipcRequest)
  }

  private val reqIdSyncObj = SynchronizedObject()
  private fun allocReqId() = synchronized(reqIdSyncObj) { ++reqId_acc }

  /**----- 发送请求 end */

  /**----- close start*/

  val isClosed = ipcLifeCycleState == IPC_STATE.CLOSED

  abstract suspend fun doClose()
  suspend fun close() {
    if (isClosed) {
      return
    }
    ipcLifeCycleState = IPC_STATE.CLOSED
    // 告知对方，我这条业务线已经准备关闭了
    this.postMessage(IpcLifeCycle(IPC_STATE.CLOSING))
    this.closeSignal.emitAndClear()

    /// 关闭的时候会自动触发销毁
    this.destroy(false)
  }

  val closeSignal = SimpleSignal()
  val onClose = this.closeSignal.toListener()


  private val _destroySignal = SimpleSignal()
  val onDestroy = this._destroySignal.toListener()

  private var _destroyed = false

  /**
   * 销毁实例
   */
  suspend fun destroy(close: Boolean = true) {
    if (_destroyed) {
      return
    }
    _destroyed = true
    if (close) {
      this.close()
    }
    this._destroySignal.emitAndClear()
  }

  /**----- close end*/
  // 标记是否启动完成
  val startDeferred = CompletableDeferred<IpcLifeCycle>()
  val isActivity get() = startDeferred.isCompleted

  suspend fun awaitStart() = startDeferred.await()

  // 告知对方我启动了
  suspend fun start() {
    ipcLifeCycleState = IPC_STATE.OPEN
    // 连接成功不管先后发送请求
    this.postMessage(IpcLifeCycle.opening())

//    ipcMessageCoroutineScope.launch {
//      val ipc = this@Ipc
//      val pingDelay = 300L
//      var fuse = 3
//      while (!this@Ipc.isActivity && !ipc.isClosed && fuse > 0) {
//        ipc.postMessage(IpcLifeCycle.opening())
//        delay(pingDelay)
//        fuse -= 1
//      }
//      // 连接失败报错
//      if (fuse <= 0) {
//        debugIpc("fuse boom ⚠️😅", "$channelId 连接无响应")
//        // 连接不上主动关闭
//        if (!ipc.startDeferred.isCompleted) {
//          ipc.startDeferred.complete(IpcLifeCycle.close())
//          ipc.close()
//        }
//        ipc.emitMessage(
//          IpcMessageArgs(
//            IpcError(
//              400,
//              "[${channelId},${remote.mmid}] ipc connection failed, no response！"
//            ), ipc
//          )
//        )
//        ipc.close()
//      }
//    }
  }

  val closing = SuspendOnce {
    if (ipcLifeCycleState !== IPC_STATE.CLOSING) {
      ipcLifeCycleState = IPC_STATE.CLOSING
      // TODO 这里是缓存消息处理的最后时间区间
      this.close()
    }
  }


  /**生命周期初始化，协商数据格式*/
  init {
    // TODO 跟对方通信 协商数据格式
    println(" xxlife listen=>🥑  ${this.channelId}")
    this.onLifeCycle { (lifeCycle, ipc) ->
      when (lifeCycle.state) {
        // 收到对方完成开始建立连接
        IPC_STATE.OPENING -> {
          ipc.postMessage(IpcLifeCycle.open()) // 解锁对方的
          ipc.startDeferred.complete(lifeCycle) // 解锁自己的
        }

        IPC_STATE.OPEN -> {
          println("xxlife open=>🍟 ${ipc.remote.mmid} ${ipc.channelId}")
          if (!ipc.startDeferred.isCompleted) {
            ipc.startDeferred.complete(lifeCycle)
          }
        }
        // 消息通道开始关闭
        IPC_STATE.CLOSING -> {
          ipc.closing()
        }
        // 对方关了，代表没有消息发过来了，我也关闭
        IPC_STATE.CLOSED -> {
          ipc.close()
        }
      }
    }
  }

//   private val readyDeferred = CompletableDeferred<IpcEvent>()
//   suspend fun afterReady() = readyDeferred.await()
//
//   // 取消等待了，再等下去没有意义
//   fun stopReady() = readyDeferred.cancel()

  /// 应用级别的 Ready协议，使用ping-pong方式来等待对方准备完毕，这不是必要的，确保双方都准寻这个协议才有必要去使用
  /// 目前使用这个协议的主要是Web端（它同时还使用了 Activity协议）
//   internal val readyPingPong = SuspendOnce1 { mm: MicroModule ->
//     this.onEvent { (event, ipc) ->
//       if (event.name == "ping") {
//         ipc.postMessage(IpcEvent("pong", event.data, event.encoding))
//       } else if (event.name == "pong") {
//         readyDeferred.complete(event)
//       }
//     }
//     mm.ioAsyncScope.launch {
//       val ipc = this@Ipc
//       val pingDelay = 200L
//       var timeout = 30000L
//       while (!readyDeferred.isCompleted && !ipc.isClosed && timeout > 0L) {
//         ipc.postMessage(IpcEvent.fromUtf8("ping", ""))
//         delay(pingDelay)
//         timeout -= pingDelay
//       }
//     }
//     readyDeferred.await()
//   }

}

data class IpcRequestInit(
  var method: PureMethod = PureMethod.GET,
  var body: IPureBody = IPureBody.Empty,
  var headers: PureHeaders = PureHeaders()
)
