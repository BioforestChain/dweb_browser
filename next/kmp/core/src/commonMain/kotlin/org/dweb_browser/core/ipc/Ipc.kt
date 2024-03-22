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
import org.dweb_browser.helper.SimpleEventFlow
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
  }

  val ipcScope = CoroutineScope(CoroutineName("ipc-$channelId") + ioAsyncExceptionHandler)

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
            ipcScope.launch {
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
          ipcScope.launch {
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
    ipcScope.launch {
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
            null -> ipcScope.launch {
              signal.emit(eventArgs)
            }
            /// 有序模式
            else -> {
              val orderedEvents = orderByChannels.getOrPut(orderBy) {
                Channel<IpcEventMessageArgs>(capacity = Channel.UNLIMITED).also { events ->
                  var lastEmitTime = 0;
                  /// 进行有序地发送
                  val sendJob = ipcScope.launch {
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
                  val gcJob = ipcScope.launch {
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
          ipcScope.launch {
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
          ipcScope.launch {
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

  // 发消息
  abstract suspend fun doPostMessage(pid: Int, data: IpcMessage)

  /**发送各类消息到remote*/
  suspend fun postMessage(data: IpcMessage) {
    if (isClosed) {
      debugIpcPool("ipc postMessage", "[$channelId] already closed:discard $data")
      return
    }
    // 等待通信建立完成（如果通道没有建立完成，并且不是生命周期消息）
    if (!isActivity && data !is IpcLifeCycle) {
      awaitStart()
    }
    // 分发消息
    this.doPostMessage(this.pid, data)
  }

  private val _messageSignal = Signal<IpcMessageArgs>()
  fun onMessage(cb: OnIpcMessage) = _messageSignal.listen(cb)

  /**分发各类消息到本地*/
  suspend fun emitMessage(args: IpcMessageArgs) = _messageSignal.emit(args)

  // 标记是否启动完成
  val startDeferred = CompletableDeferred<IpcLifeCycle>()

  // 标记ipc通道是否激活
  val isActivity get() = startDeferred.isCompleted

  suspend fun awaitStart() = startDeferred.await()

  // 告知对方我启动了
  suspend fun start() {
    ipcLifeCycleState = IPC_STATE.OPEN
    // 连接成功不管先后发送请求
    this.postMessage(IpcLifeCycle.opening())
  }

  /**生命周期初始化，协商数据格式*/
  fun initLifeCycleHook() {
    // TODO 跟对方通信 协商数据格式
    val off = this.onLifeCycle { (lifeCycle, ipc) ->
//      println("xxlife onLifeCycle=>🍟  ${ipc.channelId} ${lifeCycle.state}")
      when (lifeCycle.state) {
        // 收到对方完成开始建立连接
        IPC_STATE.OPENING -> {
          ipc.postMessage(IpcLifeCycle.open()) // 解锁对方的
          ipc.startDeferred.complete(lifeCycle) // 解锁自己的
        }

        IPC_STATE.OPEN -> {
          if (!ipc.startDeferred.isCompleted) {
            ipc.startDeferred.complete(lifeCycle)
          }
        }
        // 消息通道开始关闭
        IPC_STATE.CLOSING -> {
          debugIpc("🌼IPC close", "$channelId ${ipc.remote.mmid}")
          // 接收方接收到对方请求关闭了
          ipcLifeCycleState = IPC_STATE.CLOSING
          ipc.postMessage(IpcLifeCycle.close())
        }
        // 对方关了，代表没有消息发过来了，我也关闭
        IPC_STATE.CLOSED -> {
          debugIpc("🌼IPC destroy", "$channelId ${ipc.remote.mmid} $isClosed")
          ipc.destroy()
        }
      }
    }
    onClose {
      off()
    }
  }

  /**----- close start*/

  val isClosed get() = ipcLifeCycleState == IPC_STATE.CLOSED

  abstract suspend fun _doClose()

  // 告知对方，我这条业务线已经准备关闭了
  private suspend fun tryClose() {
    ipcLifeCycleState = IPC_STATE.CLOSING
    this.postMessage(IpcLifeCycle(IPC_STATE.CLOSING))
  }

  var _isClose = false

  // 开始触发关闭事件
  suspend fun close() {
    if (_isClose) {
      return
    }
    _isClose = true
    if (!isClosed) {
      this.tryClose()
    }
    this.destroy()
  }

  val closeSignal = SimpleEventFlow(ipcScope)
  val onClose = this.closeSignal.toListener()
  private var isDestroy = false

  init {
    println("ipc_init $channelId")
  }

  //彻底销毁
  private suspend fun destroy() {
    if (isDestroy) {
      return
    }
    isDestroy = true
    // 我彻底关闭了
    this.postMessage(IpcLifeCycle.close())
    // 开始触发各类跟ipc绑定的关闭事件
    this.closeSignal.emitAndClear()
    debugIpc("ipcDestroy=>", " $channelId 触发完成")
    this._doClose()
    // 做完全部工作了，关闭
    ipcLifeCycleState = IPC_STATE.CLOSED
  }
  /**----- close end*/
}

data class IpcRequestInit(
  var method: PureMethod = PureMethod.GET,
  var body: IPureBody = IPureBody.Empty,
  var headers: PureHeaders = PureHeaders()
)
